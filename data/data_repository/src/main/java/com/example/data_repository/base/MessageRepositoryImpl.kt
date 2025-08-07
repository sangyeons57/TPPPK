package com.example.data_repository.base

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessageIsDeleted
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.MessageRepository
import com.example.mapper.DtoMapper
import com.example.mapper.message.MessageMapper
import kotlinx.serialization.json.Json
import java.time.Instant
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
    private val entityMapper: MessageMapper, // MessageEntity <-> Message 변환용
) : DefaultRepositoryImpl<Message, MessageDTO>(messageRemoteDataSource, messageMapper),
    MessageRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
    override suspend fun sendMessage(
        channelId: String,
        content: String
    ): String {
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        db.withTransaction {
            // 1. 메시지를 Room DB에 저장 (syncStatus 없음)
            val message = Message.create(
                id = DocumentId(messageId),
                senderId = UserId(AuthUtil.getCurrentUserId() ?: ""),
                messageType = MessageType.TEXT,
                payload = MessagePayload.forText(content),
                replyToMessageId = null,
                mentions = emptyList(),
                channelId = ChannelId(channelId)
            )

            val entity = entityMapper.domainToEntity(message)
            messageDao.upsert(entity)

            // 2. OutBox에 전송 대기 레코드 생성
            val outboxRecord = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = "messages",
                aggregateId = messageId,
                op = OutBoxRecord.Op.UPSERT,
                payload = """{"channelId":"$channelId","content":"$content"}""",
                createdAt = now
            )

            outboxDao.enqueue(outboxRecord.toEntity(OutBoxStatus.PENDING))
        }

        return messageId
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
    // LocalMessageRepository 통합 메서드들
    // ================================

    override fun getMessagesPagingSource(): PagingSource<Long, Message> {
        return MessagePagingSource(db, messageDao, entityMapper, channelId = null)
    }

    override fun getMessagesPagingSource(channelId: String): PagingSource<Long, Message> {
        return MessagePagingSource(db, messageDao, entityMapper, channelId = channelId)
    }

    /**
     * Room 데이터를 Message 도메인으로 변환하는 PagingSource
     */
    private class MessagePagingSource(
        private val db: RoomDatabase,
        private val messageDao: MessageDao,
        private val messageMapper: MessageMapper,
        private val channelId: String? // null이면 모든 채널, 지정하면 해당 채널만
    ) : PagingSource<Long, Message>() {

        private val tableObserver =
            object : androidx.room.InvalidationTracker.Observer("messages") {
                override fun onInvalidated(tables: Set<String>) {
                    // 메시지 테이블 변경 시 PagingSource 새로고침
                    this@MessagePagingSource.invalidate()
                }
            }

        init {
            db.invalidationTracker.addObserver(tableObserver)
        }

        // 동일 키 재사용 허용 (프리페치/레이아웃 특성으로 동일 키가 연속 전달될 수 있음)
        override val keyReuseSupported: Boolean = true

        override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Message> {
            val channelIdParam = channelId ?: ""
            val limit = params.loadSize

            return try {
                when (params) {
                    is LoadParams.Refresh -> {
                        val beforeTs = params.key ?: Long.MAX_VALUE
                        Log.d(
                            "MessagePagingSource",
                            "🔄 Refresh: key=$beforeTs, limit=$limit, channel=$channelIdParam"
                        )
                        val entities = messageDao.getMessagesBefore(channelIdParam, beforeTs, limit)
                        val messages =
                            entities.map { entity -> messageMapper.entityToDomain(entity) }

                        val newest = entities.firstOrNull()?.createdAt
                        val oldest = entities.lastOrNull()?.createdAt

                        val prevKey = if (entities.isEmpty()) {
                            null
                        } else {
                            // 새로운 메시지를 위한 PREPEND 로딩 활성화
                            // newest 타임스탬프보다 1ms 더 큰 값으로 설정하여 새 메시지 로드 가능하게 함
                            newest?.plus(1)
                        }

                        val nextKey =
                            if (entities.isEmpty() || entities.size < limit) null else oldest?.minus(
                                1
                            )

                        Log.d(
                            "MessagePagingSource",
                            "🔄 Refresh result: size=${messages.size}, newest=$newest, oldest=$oldest, prevKey=$prevKey, nextKey=$nextKey"
                        )

                        LoadResult.Page(
                            data = messages,
                            prevKey = prevKey,
                            nextKey = nextKey
                        )
                    }

                    is LoadParams.Append -> {
                        val key = params.key
                        Log.d(
                            "MessagePagingSource",
                            "⬇️ Append: key=$key, limit=$limit, channel=$channelIdParam"
                        )
                        val entities = messageDao.getMessagesBefore(channelIdParam, key, limit)
                        val messages =
                            entities.map { entity -> messageMapper.entityToDomain(entity) }
                        val oldest = entities.lastOrNull()?.createdAt
                        val nextKey =
                            if (entities.isEmpty() || entities.size < limit) null else oldest?.minus(
                                1
                            )
                        Log.d(
                            "MessagePagingSource",
                            "⬇️ Append result: size=${messages.size}, oldest=$oldest, nextKey=$nextKey"
                        )
                        LoadResult.Page(
                            data = messages,
                            prevKey = null,
                            nextKey = nextKey
                        )
                    }

                    is LoadParams.Prepend -> {
                        val key = params.key
                        Log.d(
                            "MessagePagingSource",
                            "⬆️ Prepend: key=$key, limit=$limit, channel=$channelIdParam"
                        )
                        val entitiesAsc = messageDao.getMessagesAfter(channelIdParam, key, limit)
                        // getMessagesAfter는 ASC이므로, 전체 순서를 기존과 동일하게 DESC로 맞춤
                        val entities = entitiesAsc.sortedByDescending { it.createdAt }
                        val messages =
                            entities.map { entity -> messageMapper.entityToDomain(entity) }
                        val newest = entities.firstOrNull()?.createdAt
                        val prevKey =
                            if (entities.isEmpty() || entities.size < limit) null else newest?.plus(
                                1
                            )
                        Log.d(
                            "MessagePagingSource",
                            "⬆️ Prepend result: size=${messages.size}, newest=$newest, prevKey=$prevKey"
                        )

                        LoadResult.Page(
                            data = messages,
                            prevKey = prevKey,
                            nextKey = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MessagePagingSource", "❌ load error: ${e.message}", e)
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Long, Message>): Long? {
            // 앵커 기준으로 가장 가까운 아이템의 createdAt을 키로 사용
            return state.anchorPosition?.let { anchor ->
                state.closestItemToPosition(anchor)?.createdAt?.toEpochMilli()
            }
        }
    }

    override suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Long,
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return try {
            val entities = messageDao.getMessagesAfter(channelId, afterTimestamp, limit)
            val messages = entities.map { entity -> convertEntityToMessage(entity) }
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
            val messages = entities.map { entity -> convertEntityToMessage(entity) }
            CustomResult.Success(messages)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun getMessagesBetween(
        channelId: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): CustomResult<List<Message>, Exception> {
        return try {
            val entities = messageDao.getMessagesBetween(channelId, startTimestamp, endTimestamp)
            val messages = entities.map { entity -> convertEntityToMessage(entity) }
            CustomResult.Success(messages)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

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

    /**
     * OutBox 레코드 생성 (메시지 전송 시 PENDING 상태로 생성)
     */
    override suspend fun createOutBoxRecord(
        messageId: String,
        channelId: String,
        payload: String
    ): CustomResult<Unit, Exception> {
        return try {
            val outboxRecord = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = "messages",
                aggregateId = messageId,
                op = OutBoxRecord.Op.UPSERT,
                payload = """{"channelId":"$channelId","payload":"${
                    payload.replace("\\", "\\\\").replace("\"", "\\\"")
                }"}""",
                createdAt = System.currentTimeMillis()
            )

            outboxDao.enqueue(outboxRecord.toEntity(OutBoxStatus.PENDING))
            Log.d("MessageRepository", "OutBox record created: $messageId with status PENDING")
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to create OutBox record: $messageId", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 특정 채널의 동기화 상태별 메시지 개수 조회
     */
    override suspend fun findById(messageId: String): Message? {
        return try {
            val entity = messageDao.findById(messageId)
            entity?.let { convertEntityToMessage(it) }
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
            val messages = entities.map { entity -> convertEntityToMessage(entity) }

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


    /**
     * MessageEntity를 Message 도메인 객체로 변환
     */
    private fun convertEntityToMessage(entity: MessageEntity): Message {
        return Message.fromDataSource(
            id = DocumentId(entity.id),
            senderId = UserId(entity.senderId),
            messageType = try {
                MessageType.valueOf(entity.messageType)
            } catch (e: Exception) {
                MessageType.TEXT
            },
            payload = MessagePayload(entity.payload),
            replyToMessageId = entity.replyToMessageId?.let { DocumentId(it) },
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt),
            isDeleted = MessageIsDeleted.fromBoolean(entity.isDeleted),
            mentions = emptyList(), // TODO: JSON 파싱하여 mentions 복원
            channelId = ChannelId(entity.channelId)
        )
    }
}
