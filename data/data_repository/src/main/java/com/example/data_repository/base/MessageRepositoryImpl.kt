package com.example.data_repository.base

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
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.MessageRepository
import com.example.mapper.DtoMapper
import com.example.mapper.message.MessageMapper
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Date
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
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        db.withTransaction {
            messageDao.upsert(
                MessageEntity(
                    id = id,
                    channelId = channelId,
                    content = content,
                    updatedAt = now,
                    createdAt = now,
                    syncStatus = SyncStatus.PENDING.name,
                    senderId = AuthUtil.getCurrentUserId(),
                )
            )

            val payload = json.encodeToString(
                MessageDTO(
                    id = id,
                    channelId = channelId,
                    senderId = AuthUtil.getCurrentUserId(),
                    content = content,
                    updatedAt = Date(now),
                    createdAt = Date(now),
                )
            )

            outboxDao.enqueue(
                OutBoxRecord(
                    id = UUID.randomUUID().toString(),
                    stream = Message.COLLECTION_NAME,
                    aggregateId = id,
                    op = OutBoxRecord.Op.UPSERT,
                    payload = payload,
                    createdAt = now,
                ).toEntity()
            )
        }

        // ✅ Room 저장 직후 전체 메시지 로그 출력
        val allMessages = messageDao.getRecentMessagesForDebug(channelId, 20)
        messageDao.getTotalMessageCount()
        messageDao.getMessageCountByChannel(channelId)

        android.util.Log.d(
            "RoomDB",
            "💾 Room 저장 후 상태 - 전체: $totalCount개, 채널($channelId): $channelCount개"
        )
        android.util.Log.d("RoomDB", "💾 Room 저장 후 최근 메시지 (채널: $channelId): ${allMessages.size}개")
        allMessages.forEach { entity ->
            android.util.Log.d(
                "RoomDB",
                "  - id: ${entity.id}, content: ${entity.content.take(30)}, syncStatus: ${entity.syncStatus}"
            )
        }

        return id
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
        return MessagePagingSource(messageDao, entityMapper, channelId = null)
    }

    override fun getMessagesPagingSource(channelId: String): PagingSource<Long, Message> {
        return MessagePagingSource(messageDao, entityMapper, channelId = channelId)
    }

    /**
     * Room 데이터를 Message 도메인으로 변환하는 PagingSource
     */
    private class MessagePagingSource(
        private val messageDao: MessageDao,
        private val messageMapper: MessageMapper,
        private val channelId: String? // null이면 모든 채널, 지정하면 해당 채널만
    ) : PagingSource<Long, Message>() {

        override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Message> {
            return try {
                val key = params.key ?: System.currentTimeMillis()
                val limit = params.loadSize
                val targetChannelId = channelId ?: "" // 빈 문자열은 모든 채널 (DAO에서 처리)

                android.util.Log.d(
                    "PagingSource",
                    "🔄 PagingSource.load() 호출됨 - 채널: $targetChannelId, 키: $key, 크기: $limit"
                )

                // ✅ Room DB 상태 직접 확인
                messageDao.getTotalMessageCount()
                messageDao.getMessageCountByChannel(targetChannelId)
                android.util.Log.d(
                    "PagingSource",
                    "📊 Room DB 상태 - 전체: $totalCount개, 채널($targetChannelId): $channelCount개"
                )

                // 기존 DAO 메서드 활용 - 최신 메시지부터 시간 역순으로 로딩
                val entities = when (params) {
                    is LoadParams.Refresh -> {
                        android.util.Log.d("PagingSource", "📥 Refresh 로드 - 현재 시간 이전의 최신 메시지들")
                        android.util.Log.d(
                            "PagingSource",
                            "📋 쿼리 조건: channelId='$targetChannelId', beforeTimestamp=$key, limit=$limit"
                        )
                        // 초기 로드: 현재 시간 이전의 최신 메시지들
                        val result = messageDao.getMessagesBefore(targetChannelId, key, limit)
                        android.util.Log.d("PagingSource", "📥 Refresh 쿼리 결과: ${result.size}개")
                        result
                    }

                    is LoadParams.Prepend -> {
                        android.util.Log.d("PagingSource", "📥 Prepend 로드 - 더 최신 메시지들")
                        android.util.Log.d(
                            "PagingSource",
                            "📋 쿼리 조건: channelId='$targetChannelId', afterTimestamp=$key, limit=$limit"
                        )
                        // 위로 스크롤: 더 최신 메시지들
                        val result = messageDao.getMessagesAfter(targetChannelId, key, limit)
                        android.util.Log.d("PagingSource", "📥 Prepend 쿼리 결과: ${result.size}개")
                        result
                    }

                    is LoadParams.Append -> {
                        android.util.Log.d("PagingSource", "📥 Append 로드 - 더 과거 메시지들")
                        android.util.Log.d(
                            "PagingSource",
                            "📋 쿼리 조건: channelId='$targetChannelId', beforeTimestamp=$key, limit=$limit"
                        )
                        // 아래로 스크롤: 더 과거 메시지들
                        val result = messageDao.getMessagesBefore(targetChannelId, key, limit)
                        android.util.Log.d("PagingSource", "📥 Append 쿼리 결과: ${result.size}개")
                        result
                    }
                }

                android.util.Log.d("PagingSource", "✅ DAO 쿼리 완료 - ${entities.size}개 엔티티 로드됨")

                // MessageEntity를 Message 도메인으로 변환
                val messages = entities.map { entity ->
                    messageMapper.entityToDomain(entity)
                }

                // ✅ Paging3로 UI에 출력되는 메시지 로그
                android.util.Log.d(
                    "Paging3",
                    "🖥️ Paging3로 UI에 출력되는 메시지 (채널: $targetChannelId): ${messages.size}개"
                )
                messages.forEach { message ->
                    android.util.Log.d(
                        "Paging3",
                        "  - id: ${message.id.value}, content: ${message.content.value.take(30)}, channelId: ${message.channelId.value}"
                    )
                }

                // 다음/이전 키 계산
                val nextKey = entities.lastOrNull()?.createdAt
                val prevKey = entities.firstOrNull()?.createdAt

                android.util.Log.d(
                    "PagingSource",
                    "📊 LoadResult 생성 - prevKey: $prevKey, nextKey: $nextKey"
                )

                LoadResult.Page(
                    data = messages,
                    prevKey = if (params is LoadParams.Refresh || params is LoadParams.Append) prevKey else null,
                    nextKey = if (messages.isEmpty()) null else nextKey
                )
            } catch (e: Exception) {
                android.util.Log.e("PagingSource", "❌ PagingSource.load() 실패", e)
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: androidx.paging.PagingState<Long, Message>): Long? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
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

    override suspend fun updateSyncStatus(
        messageId: DocumentId,
        syncStatus: SyncStatus
    ): CustomResult<Unit, Exception> {
        return try {
            messageDao.updateSyncStatus(messageId.value, syncStatus.name)
            CustomResult.Success(Unit)
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

                android.util.Log.d("MessageRepository", "=== 로컬 캐시 완전 클리어 완료 ===")
                android.util.Log.d("MessageRepository", "채널: $channelId")
                android.util.Log.d("MessageRepository", "삭제된 메시지: $deletedMessagesCount 개")
                android.util.Log.d("MessageRepository", "삭제된 OutBox 레코드: $deletedOutboxCount 개")
                android.util.Log.d("MessageRepository", "리셋된 동기화 커서: $resetSyncCount 개")
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MessageRepository", "로컬 캐시 클리어 실패", e)
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

                android.util.Log.d("MessageRepository", "=== 전체 캐시 완전 클리어 완료 ===")
                android.util.Log.d("MessageRepository", "삭제된 메시지: $deletedMessagesCount 개")
                android.util.Log.d("MessageRepository", "삭제된 OutBox 레코드: $deletedOutboxCount 개")
                android.util.Log.d("MessageRepository", "삭제된 동기화 메타데이터: $deletedSyncCount 개")
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MessageRepository", "전체 캐시 클리어 실패", e)
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
                android.util.Log.e("MessageRepository", "ChannelId cannot be blank")
                return CustomResult.Failure(
                    IllegalArgumentException("ChannelId cannot be blank")
                )
            }

            // MessageRemoteDataSource를 통해 Firestore에서 최신 메시지 가져오기
            messageRemoteDataSource.getRecentMessages(channelId, limit)
        } catch (e: Exception) {
            android.util.Log.e(
                "MessageRepository",
                "Failed to get recent messages for channel: $channelId",
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
            senderId = com.example.domain.model.vo.UserId(entity.senderId),
            content = com.example.domain.model.vo.message.MessageContent(entity.content),
            replyToMessageId = entity.replyToMessageId?.let { DocumentId(it) },
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt),
            isDeleted = com.example.domain.model.vo.message.MessageIsDeleted.fromBoolean(entity.isDeleted),
            mentions = emptyList(), // TODO: JSON 파싱하여 mentions 복원
            channelId = com.example.domain.model.vo.ChannelId(entity.channelId)
        )
    }
}
