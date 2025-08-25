package com.example.data_repository.base

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.core_common.result.CustomResult
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
import com.example.domain.vo.DocumentId
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
    override suspend fun sendMessage(entity: Message): CustomResult<DocumentId, Exception> {
        return try {
            db.withTransaction {
                val entityModel = entityMapper.domainToEntity(entity)

                messageDao.upsert(entityModel)
                Log.d(
                    "MessageRepository",
                    "💾 Upsert(Room) message id=${entityModel.id}, channel=${entityModel.channelId}, updatedAt=${entityModel.updatedAt}"
                )

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
                Log.d(
                    "MessageRepository",
                    "📤 Enqueued OutBox UPSERT id=${outboxRecord.id}, msg=${outboxRecord.aggregateId}, channel=${entityModel.channelId}"
                )
            }
            CustomResult.Success(entity.id)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Room save(upsert) 실패: ${entity.id.value}", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * save() 메서드 사용 금지 - 메시지는 반드시 sendMessage() 사용
     *
     * 이유:
     * - save()는 단순 저장만 하고 WebSocket 전송을 하지 않음
     * - sendMessage()는 로컬 저장 + WebSocket 전송을 모두 처리
     * - 메시지 전송은 반드시 WebSocket을 통해 실시간으로 전달되어야 함
     */
    override suspend fun save(entity: Message): CustomResult<DocumentId, Exception> {
        val errorMessage = """
            ❌ MessageRepository에서 save() 사용 금지!
            
            메시지 전송은 반드시 sendMessage()를 사용하세요:
            - save(): 로컬 저장만 (WebSocket 전송 없음)
            - sendMessage(): 로컬 저장 + WebSocket 전송 (권장)
            
            올바른 사용법:
            messageRepository.sendMessage(message)
        """.trimIndent()

        Log.e("MessageRepository", errorMessage)
        return CustomResult.Failure(
            UnsupportedOperationException(
                "Use sendMessage() instead of save() for message transmission with WebSocket"
            )
        )
    }

    /**
     * WebSocket으로부터 수신된 메시지를 로컬에 저장합니다.
     *
     * 이 메서드는 WebSocket 서비스 전용이며, 다음 용도로만 사용됩니다:
     * - 서버로부터 수신된 메시지의 로컬 저장
     * - WebSocket 전송 없이 순수 저장만 수행
     *
     * 일반적인 메시지 전송에는 sendMessage()를 사용하세요.
     */
    override suspend fun saveReceivedMessage(entity: Message): CustomResult<DocumentId, Exception> {
        Log.d("MessageRepository", "saveReceivedMessage(Room) 호출: ${entity.id.value}")

        return try {
            // 원격(Firestore)을 건드리지 않고, 로컬(Room)만 업서트한다.
            db.withTransaction {
                val entityModel = entityMapper.domainToEntity(entity)

                messageDao.upsert(entityModel)
            }
            CustomResult.Success(entity.id)
        } catch (e: Exception) {
            Log.e("MessageRepository", "saveReceivedMessage(Room) 실패: ${entity.id.value}", e)
            CustomResult.Failure(e)
        }
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
            // 채널ID를 OutBox payload에 포함해야 원격 push 경로를 계산할 수 있음
            val existing = runCatching { messageDao.findById(id) }.getOrNull()
            if (existing == null) {
                Log.w(
                    "MessageRepository",
                    "⚠️ deleteMessage: local entity not found, skip OutBox enqueue. id=$id"
                )
                // 여전히 tombstone 시도하여 UI 상에서 제거 효과는 유지
                messageDao.tombstone(id, now)
                return@withTransaction
            }

            // 1) 로컬 tombstone 처리
            messageDao.tombstone(id, now)
            Log.d(
                "MessageRepository",
                "🪦 Tombstoned(Room) message id=$id, channel=${existing.channelId}, ts=$now"
            )

            // 2) OutBox DELETE enqueue (channelId 포함 필수)
            val payload = """
                {
                  "id":"$id",
                  "channelId":"${existing.channelId}",
                  "isDeleted":true,
                  "deletedAt":$now
                }
            """.trimIndent()
            val record = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = Message.COLLECTION_NAME,
                aggregateId = id,
                op = OutBoxRecord.Op.DELETE,
                payload = payload,
                createdAt = now
            )
            outboxDao.enqueue(record.toEntity())
            Log.d(
                "MessageRepository",
                "🗑️ Enqueued OutBox DELETE id=${record.id}, msg=${record.aggregateId}, channel=${existing.channelId}"
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
        Log.d("MessageRepository", "📚 Create PagingSource for channel=$channelId")
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
            Log.d(
                "MessageRepository",
                "🔎 getMessagesAfter channel=$channelId after=$afterTimestamp limit=$limit"
            )
            val entities = messageDao.getMessagesAfter(channelId, afterTimestamp, limit)
            val messages = entities.map { entity -> entityMapper.entityToDomain(entity) }
            Log.d(
                "MessageRepository",
                "🔎 getMessagesAfter -> ${messages.size} item(s)"
            )
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
                // Message의 updatedAt도 변경하여 Room이 변경사항을 감지하도록 함
                messageDao.updateTimestamp(messageId, java.time.Instant.now().toEpochMilli())
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
