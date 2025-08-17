package com.example.data_repository.base

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.data_datasource.remote.MessageRemoteDataSource
import com.example.data_model.local.MessageDao
import com.example.data_model.local.MessageEntity
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.SyncMetadataDao
import com.example.data_model.local.toEntity
import com.example.data_model.remote.MessageDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.data_repository.util.OutboxPayloadUtil
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.MessageRepository
import com.example.mapper.DtoMapper
import com.example.mapper.message.MessageMapper
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val db: RoomDatabase,
    private val messageDao: MessageDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val json: Json,

    private val messageMapper: DtoMapper<Message, MessageDTO>,
    private val entityMapper: MessageMapper // MessageEntity <-> Message 변환용
) : DefaultRepositoryImpl<Message, MessageDTO>(messageRemoteDataSource, messageMapper),
    MessageRepository {
    /**
     * 로컬(Room) 우선 저장 업서트 구현
     * - 메시지 저장은 Room DAO를 통해 즉시 반영한다
     * - 원격 저장은 WebSocket 경로에서 서버가 책임진다
     */
    override suspend fun save(entity: Message): CustomResult<DocumentId, Exception> {
        return try {
            db.withTransaction {
                val entityModel = entityMapper.domainToEntity(entity)

                // 중복 방지: 동일 채널/보낸이/페이로드가 이미 존재하면 이전 레코드를 정리
                runCatching {
                    val existingId = messageDao.findExistingIdBySenderAndPayload(
                        channelId = entityModel.channelId,
                        senderId = entityModel.senderId,
                        payload = entityModel.payload
                    )
                    if (existingId != null && existingId != entityModel.id) {
                        // 이전(중복) 레코드를 tombstone 처리하여 UI 중복 제거
                        messageDao.tombstone(existingId, entityModel.updatedAt)
                    }
                }.onFailure {
                    Log.w("MessageRepository", "중복 검사 실패(무시): ${it.message}")
                }

                messageDao.upsert(entityModel)

                // OutBox UPSERT 레코드 생성 (로컬 저장과 같은 트랜잭션)
                val outboxPayload = OutboxPayloadUtil.toPayload(entityModel)
                val outboxRecord = OutBoxRecord(
                    id = UUID.randomUUID().toString(),
                    stream = Message.COLLECTION_NAME,
                    aggregateId = entityModel.id,
                    op = OutBoxRecord.Op.UPSERT,
                    payload = outboxPayload,
                    createdAt = System.currentTimeMillis()
                )
                outboxDao.enqueue(outboxRecord.toEntity(OutBoxStatus.PENDING))
            }
            CustomResult.Success(entity.id)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Room save(upsert) 실패: ${entity.id.value}", e)
            CustomResult.Failure(e)
        }
    }

    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
    override suspend fun sendMessage(
        channelId: String,
        payload: MessagePayload
    ): String {
        val messageId = UUID.randomUUID().toString()
        System.currentTimeMillis()

        val message = Message.create(
            id = DocumentId(messageId),
            senderId = UserId(AuthUtil.getCurrentUserId() ?: ""),
            messageType = MessageType.TEXT,
            payload = payload,
            replyToMessageId = null,
            mentions = emptyList(),
            channelId = ChannelId(channelId)
        )
        // save() 내부에서 트랜잭션 + OutBox enqueue 처리
        save(message)

        return messageId
    }

    /**
     * DefaultRepository의 원격 기반 findById(id, source) 호출이 들어오더라도
     * MessageRepository는 원격을 사용하지 않고 로컬(Room) DB만 조회하도록 강제합니다.
     * source 파라미터는 무시되며, 명시적으로 로그를 남깁니다.
     */
    override suspend fun findById(
        id: DocumentId,
        source: Source
    ): CustomResult<Message, Exception> {
        return try {
            Log.d(
                "MessageRepository",
                "findById(DocumentId) -> Using ROOM DB only (ignoring remote/source). id=${id.value}, source=$source"
            )
            val entity = messageDao.findById(id.value)
            if (entity != null) {
                CustomResult.Success(entityMapper.entityToDomain(entity))
            } else {
                Log.w("MessageRepository", "Local(Room) message not found: ${id.value}")
                CustomResult.Failure(IllegalStateException("Message not found locally: ${id.value}"))
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Local(Room) findById failed: ${id.value}", e)
            CustomResult.Failure(e)
        }
    }


    override suspend fun deleteMessage(id: String) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            messageDao.tombstone(id, now)
            val payload = """{"id":"$id","deletedAt":$now}"""
            outboxDao.enqueue(
                OutBoxRecord(
                    id = UUID.randomUUID().toString(),
                    stream = Message.COLLECTION_NAME,
                    aggregateId = id,
                    op = OutBoxRecord.Op.DELETE,
                    payload = payload,
                    createdAt = now
                ).toEntity()
            )
        }
    }

    // ================================
    // Room DAO PagingSource 지원 메서드들
    // ================================

    /**
     * MessageEntity PagingSource를 반환하고 변환은 상위 레이어에서 처리
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getMessageEntityPagingSource(channelId: String): PagingSource<Int, T> {
        require(channelId.isNotBlank()) { "channelId must not be blank for message paging" }
        return messageDao.pagingSource(channelId) as PagingSource<Int, T>
    }

    /**
     * MessageEntity를 Message로 변환하는 매퍼 노출
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> convertEntityToDomain(entity: T): Message {
        return entityMapper.entityToDomain(entity as MessageEntity)
    }

    override suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Long,
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return try {
            val entities = messageDao.getMessagesAfter(channelId, afterTimestamp, limit)
            val messages = entities.map { entity -> entityMapper.entityToDomain(entity) }
            CustomResult.Success(messages)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Long,
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return try {
            val entities = messageDao.getMessagesBefore(channelId, beforeTimestamp, limit)
            val messages = entities.map { entity -> entityMapper.entityToDomain(entity) }
            CustomResult.Success(messages)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // 사용하지 않는 메서드 제거됨: getMessagesBetween

    // ================================
    // OutBox 기반 동기화 상태 관리
    // ================================

    /**
     * OutBox 상태를 기반으로 메시지의 동기화 상태 조회
     */
    override suspend fun getMessageOutBoxStatus(messageId: DocumentId): CustomResult<OutBoxStatus, Exception> {
        return try {
            val outboxRecord = outboxDao.findByMessageId(messageId.value)
            val outBoxStatus = OutBoxStatus.fromString(outboxRecord?.status)
            CustomResult.Success(outBoxStatus)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 메시지 ACK 처리 (WebSocket ACK 수신 시)
     */
    override suspend fun handleMessageAck(messageId: String): CustomResult<Unit, Exception> {
        return try {
            val updatedCount = outboxDao.markMessageDispatched(messageId)
            if (updatedCount > 0) {
                Log.d("MessageRepository", "Message ACK processed: $messageId")
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to handle message ACK: $messageId", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 메시지 실패 처리 (WebSocket FAILED 수신 시)
     */
    override suspend fun handleMessageFailure(messageId: String): CustomResult<Unit, Exception> {
        return try {
            val updatedCount = outboxDao.markFailed(listOf(messageId))
            if (updatedCount > 0) {
                Log.d("MessageRepository", "Message failure processed: $messageId")
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to handle message failure: $messageId", e)
            CustomResult.Failure(e)
        }
    }

    // ================================
    // OutBox 상태 관찰 Flow API
    // ================================

    override fun observeMessageOutBoxStatus(messageId: DocumentId): Flow<OutBoxStatus> {
        return outboxDao.observeStatusByMessageId(messageId.value)
            .map { status -> OutBoxStatus.fromString(status) }
    }

    override fun observeChannelPendingCount(channelId: String): Flow<Int> {
        return outboxDao.observePendingCountByChannel(channelId)
    }

    override fun observeChannelOutBoxStatuses(channelId: String): Flow<Map<String, OutBoxStatus>> {
        return outboxDao.observeStatusesByChannel(channelId).map { rows ->
            rows.associate { it.messageId to OutBoxStatus.fromString(it.status) }
        }
    }

    // createOutBoxRecord 제거: save/sendMessage 경로에서 트랜잭션으로 자동 enqueue 처리

    /**
     * 특정 채널의 동기화 상태별 메시지 개수 조회
     */
    override suspend fun findById(messageId: String): Message? {
        return try {
            Log.d(
                "MessageRepository",
                "findById(local Room) called: id=$messageId"
            )
            val entity = messageDao.findById(messageId)
            entity?.let { entityMapper.entityToDomain(it) }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to find message by ID: $messageId", e)
            null
        }
    }

    override suspend fun getChannelOutBoxStatusCounts(channelId: String): CustomResult<Map<OutBoxStatus, Int>, Exception> {
        return try {
            val statusCounts = outboxDao.getOutBoxStatusCountsByChannel(channelId)
            val result = mutableMapOf<OutBoxStatus, Int>()

            statusCounts.forEach { statusCount ->
                val outBoxStatus = OutBoxStatus.fromString(statusCount.status)
                result[outBoxStatus] = statusCount.count
            }

            CustomResult.Success(result)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 캐시 관리 기능
    // ================================

    override suspend fun clearLocalCache(channelId: String): CustomResult<Unit, Exception> {
        return try {
            db.withTransaction {
                // 1. 해당 채널의 모든 메시지 삭제
                val deletedMessagesCount = messageDao.deleteAllByChannelId(channelId)

                // 2. 해당 채널과 관련된 OutBox 레코드 삭제
                val deletedOutboxCount = outboxDao.deleteByChannelId(channelId)

                // 3. messages 스트림의 동기화 메타데이터 리셋 (처음부터 다시 동기화)
                val resetSyncCount = syncMetadataDao.resetCursor("messages")

                Log.d("MessageRepository", "=== 로컬 캐시 완전 클리어 완료 ===")
                Log.d("MessageRepository", "채널: $channelId")
                Log.d("MessageRepository", "삭제된 메시지: $deletedMessagesCount 개")
                Log.d("MessageRepository", "삭제된 OutBox 레코드: $deletedOutboxCount 개")
                Log.d("MessageRepository", "리셋된 동기화 커서: $resetSyncCount 개")
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("MessageRepository", "로컬 캐시 클리어 실패", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun clearAllCache(): CustomResult<Unit, Exception> {
        return try {
            db.withTransaction {
                // 1. 모든 메시지 삭제 (채널 구분 없이)
                val deletedMessagesCount = messageDao.deleteAll()

                // 2. 모든 OutBox 레코드 삭제
                val deletedOutboxCount = outboxDao.deleteAll()

                // 3. 모든 동기화 메타데이터 삭제
                val deletedSyncCount = syncMetadataDao.deleteAll()

                Log.d("MessageRepository", "=== 전체 캐시 완전 클리어 완료 ===")
                Log.d("MessageRepository", "삭제된 메시지: $deletedMessagesCount 개")
                Log.d("MessageRepository", "삭제된 OutBox 레코드: $deletedOutboxCount 개")
                Log.d("MessageRepository", "삭제된 동기화 메타데이터: $deletedSyncCount 개")
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("MessageRepository", "전체 캐시 클리어 실패", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun getRecentMessages(
        channelId: String,
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return try {
            // channelId 유효성 검사
            if (channelId.isBlank()) {
                Log.e("MessageRepository", "ChannelId cannot be blank")
                return CustomResult.Failure(
                    IllegalArgumentException("ChannelId cannot be blank")
                )
            }

            Log.d(
                "MessageRepository",
                "📱 Room DB에서 최신 메시지 조회 - 채널: $channelId, 제한: $limit"
            )

            // Room DB에서 최신 메시지들 가져오기 (현재 시간 이전의 메시지들)
            val currentTime = System.currentTimeMillis()
            val entities = messageDao.getMessagesBefore(channelId, currentTime, limit)
            val messages = entities.map { entity -> entityMapper.entityToDomain(entity) }

            Log.d(
                "MessageRepository",
                "✅ Room DB에서 ${messages.size}개 메시지 조회 완료 (채널: $channelId)"
            )

            CustomResult.Success(messages)
        } catch (e: Exception) {
            Log.e(
                "MessageRepository",
                "❌ Room DB에서 최신 메시지 조회 실패 - 채널: $channelId",
                e
            )
            CustomResult.Failure(e)
        }
    }

}
