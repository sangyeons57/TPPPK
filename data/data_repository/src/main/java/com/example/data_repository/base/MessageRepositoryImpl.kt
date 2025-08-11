package com.example.data_repository.base

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.core_common.result.CustomResult
import com.example.core_common.constant.PagingConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.core_common.util.AuthUtil
import com.example.core_common.util.SyncThrottler
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
import com.example.domain.model.sync.SyncCursorStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
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

    // 동기화 관련 의존성
    private val syncThrottler: SyncThrottler,
    private val cursorStore: SyncCursorStore
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
                messageDao.upsert(entityModel)
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
        payload: Map<String, Any?>
    ): String {
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        db.withTransaction {
            // 1. 메시지를 Room DB에 저장 (syncStatus 없음)
            val message = Message.create(
                id = DocumentId(messageId),
                senderId = UserId(AuthUtil.getCurrentUserId() ?: ""),
                messageType = MessageType.TEXT,
                payload = MessagePayload(payload.toJsonString()),
                replyToMessageId = null,
                mentions = emptyList(),
                channelId = ChannelId(channelId)
            )

            val entity = entityMapper.domainToEntity(message)
            messageDao.upsert(entity)

            // 2. OutBox에 전송 대기 레코드 생성 (플랫 JSON: channelId + payload 필드 병합)
            val mergedOutboxPayload = buildOutboxPayload(channelId, payload)
            val outboxRecord = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = "messages",
                aggregateId = messageId,
                op = OutBoxRecord.Op.UPSERT,
                payload = mergedOutboxPayload,
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

    private var currentPagingSource: MessagePagingSource? = null

    override fun getMessagesPagingSource(channelId: String): PagingSource<Long, Message> {
        require(channelId.isNotBlank()) { "channelId must not be blank for message paging" }
        val newPagingSource = MessagePagingSource(
            db = db,
            messageDao = messageDao,
            messageMapper = entityMapper,
            channelId = channelId,
            syncThrottler = syncThrottler,
            cursorStore = cursorStore
        )
        currentPagingSource = newPagingSource
        return newPagingSource
    }

    /**
     * 현재 PagingSource를 강제로 invalidate (ACK 처리 후 UI 즉시 갱신용)
     */
    fun invalidateCurrentPagingSource() {
        currentPagingSource?.invalidate()
        Log.d("MessageRepository", "🔄 PagingSource 강제 invalidate 실행")
    }

    /**
     * Room 데이터를 Message 도메인으로 변환하는 PagingSource
     */
    private class MessagePagingSource(
        private val db: RoomDatabase,
        private val messageDao: MessageDao,
        private val messageMapper: MessageMapper,
        private val channelId: String, // 빈 문자열이면 모든 채널, 지정하면 해당 채널만
        private val syncThrottler: SyncThrottler,
        private val cursorStore: SyncCursorStore
    ) : PagingSource<Long, Message>() {

        // 디바운싱을 위한 코루틴 스코프
        private val debounceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var invalidationJob: Job? = null
        private var isLoadingInProgress = false

        // 로그 최적화를 위한 변수들
        private var lastInvalidationLogTime = 0L
        private var lastLoadLogTime = 0L
        
        private val tableObserver =
            object : androidx.room.InvalidationTracker.Observer("messages", "outboxRecord") {
                override fun onInvalidated(tables: Set<String>) {
                    val currentTime = System.currentTimeMillis()
                    // 무효화 로그는 1초에 한 번만 출력
                    if (currentTime - lastInvalidationLogTime > 1000L) {
                        Log.d(
                            "MessagePagingSource",
                            "🔔 Room Invalidation: tables=${tables.joinToString()} (channel=$channelId)"
                        )
                        lastInvalidationLogTime = currentTime
                    }
                    
                    // 진행 중인 로딩이 있으면 무효화 지연
                    if (isLoadingInProgress) {
                        return
                    }

                    // 디바운싱: 100ms 내에 추가 변경이 없으면 무효화 실행 (빠른 반응성)
                    invalidationJob?.cancel()
                    invalidationJob = debounceScope.launch {
                        delay(100L)
                        // 디바운싱 실행 로그도 간소화
                        if (currentTime - lastInvalidationLogTime > 500L) {
                            Log.d("MessagePagingSource", "🔄 디바운싱 무효화: $channelId")
                        }
                        this@MessagePagingSource.invalidate()
                    }
                }
            }

        init {
            db.invalidationTracker.addObserver(tableObserver)
        }

        // 동일 키 재사용 허용 (프리페치/레이아웃 특성으로 동일 키가 연속 전달될 수 있음)
        override val keyReuseSupported: Boolean = true

        // 무한 로딩 방지를 위한 상수들
        companion object {
            private val MAX_SIZE = PagingConstants.MAX_SIZE
            private val MAX_SIZE_THRESHOLD = PagingConstants.MAX_SIZE_THRESHOLD
        }

        // 현재 로딩된 아이템 수 추적 (무한 로딩 방지용)
        private var currentLoadedItems = 0
        
        // 동시 로딩 방지를 위한 Mutex
        private val loadingMutex = Mutex()
        
        // 무한 루프 감지를 위한 변수들
        private var consecutiveEmptyLoads = 0
        private var lastLoadTimestamp = 0L
        private val MAX_CONSECUTIVE_EMPTY_LOADS = 3
        private val MIN_LOAD_INTERVAL_MS = 100L

        override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Message> {
            return loadingMutex.withLock {
                isLoadingInProgress = true
                try {
                    loadInternal(params)
                } finally {
                    isLoadingInProgress = false
                }
            }
        }
        
        private suspend fun loadInternal(params: LoadParams<Long>): LoadResult<Long, Message> {
            val channelIdParam = channelId
            val limit = params.loadSize
            val currentTime = System.currentTimeMillis()

            return try {
                // 무한 루프 감지: 너무 빠른 연속 로딩 방지
                if (params !is LoadParams.Refresh) {
                    if (currentTime - lastLoadTimestamp < MIN_LOAD_INTERVAL_MS) {
                        consecutiveEmptyLoads++
                        // 빠른 연속 로딩 로그를 1초에 한 번만 출력
                        if (currentTime - lastLoadLogTime > 1000L) {
                            Log.d(
                                "MessagePagingSource",
                                "⚠️ 빠른 연속 로딩 감지: 간격=${currentTime - lastLoadTimestamp}ms, 연속=${consecutiveEmptyLoads}"
                            )
                            lastLoadLogTime = currentTime
                        }

                        if (consecutiveEmptyLoads >= MAX_CONSECUTIVE_EMPTY_LOADS) {
                            Log.w(
                                "MessagePagingSource",
                                "🚫 무한 루프 차단: 연속 빈 로딩 ${consecutiveEmptyLoads}회"
                            )
                            return LoadResult.Page(
                                data = emptyList(),
                                prevKey = null,
                                nextKey = null
                            )
                        }
                    } else {
                        consecutiveEmptyLoads = 0 // 간격이 충분하면 리셋
                    }
                }
                lastLoadTimestamp = currentTime

                // Append 무제한 진행: 임계값 도달 시 차단하지 않고 Room 데이터 끝까지 로드

                // 동시 로딩 방지 로직 제거 (단순화)

                // 상태별 전용 처리 함수로 위임
                when (params) {
                    is LoadParams.Refresh -> return handleRefresh(params.key, limit, channelIdParam)
                    is LoadParams.Prepend -> return handlePrepend(params.key, limit, channelIdParam)
                    is LoadParams.Append -> return handleAppend(params.key, limit, channelIdParam)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MessagePagingSource", "❌ load error: ${e.message}", e)
                LoadResult.Error(e)
            }
        }

        private suspend fun handleRefresh(anchorTimestampParam: Long?, limit: Int, channelIdParam: String): LoadResult<Long, Message> {
            val anchorTimestamp = anchorTimestampParam ?: Long.MAX_VALUE
            Log.d(
                "MessagePagingSource",
                "🔄 Refresh 시작(양방향): anchorTs=$anchorTimestamp, limit=$limit, channel=$channelIdParam"
            )

            // 양방향 로딩: 앵커 기준으로 최신/과거 양쪽 데이터 로드
            val halfLimit = (limit / 2).coerceAtLeast(10) // 최소 10개씩

            // 1. 앵커 이후 데이터 (최신 방향) - ASC 순서로 가져온 후 DESC로 뒤집기
            val afterAsc = messageDao.getMessagesAfter(channelIdParam, anchorTimestamp, halfLimit)
            val afterDesc = afterAsc.reversed()

            // 2. 앵커 이전 데이터 (과거 방향) - 이미 DESC 순서
            val remainingLimit = limit - afterDesc.size
            val beforeDesc = if (remainingLimit > 0) {
                messageDao.getMessagesBefore(channelIdParam, anchorTimestamp, remainingLimit)
            } else {
                emptyList()
            }

            // 3. 데이터 결합 (최신 → 과거 순서 유지)
            val combinedEntities = afterDesc + beforeDesc
            val messages = combinedEntities.map { entity -> messageMapper.entityToDomain(entity) }

            // 4. 동기화 확인 및 실행 (데이터 부족 시)
            checkAndSyncIfNeeded(
                channelId = channelIdParam,
                resultSize = messages.size,
                requestedLimit = limit,
                timestamp = anchorTimestamp,
                loadType = "Refresh"
            )

            currentLoadedItems = messages.size
            if (messages.isNotEmpty()) consecutiveEmptyLoads = 0

            val newestTimestamp = combinedEntities.firstOrNull()?.createdAt
            val oldestTimestamp = combinedEntities.lastOrNull()?.createdAt

            // 양방향 키 설정
            val prevKey: Long? = if (afterDesc.isEmpty() || afterDesc.size < halfLimit) {
                null // 더 최신 데이터 없음
            } else {
                newestTimestamp
            }

            val nextKey: Long? = if (beforeDesc.isEmpty() || beforeDesc.size < remainingLimit) {
                null // 더 과거 데이터 없음
            } else {
                oldestTimestamp
            }

            Log.d(
                "MessagePagingSource",
                "🔄 Refresh(양방향) 결과: total=${messages.size} (최신=${afterDesc.size}, 과거=${beforeDesc.size}), newest=$newestTimestamp, oldest=$oldestTimestamp, prevKey=$prevKey, nextKey=$nextKey"
            )

            return LoadResult.Page(
                data = messages,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }

        private suspend fun handlePrepend(afterTimestamp: Long?, limit: Int, channelIdParam: String): LoadResult<Long, Message> {
            Log.d(
                "MessagePagingSource",
                "⬆️ Prepend (최신 메시지 로딩): afterTs=$afterTimestamp, limit=$limit, channel=$channelIdParam"
            )

            val entitiesAsc = messageDao.getMessagesAfter(channelIdParam, afterTimestamp ?: 0L, limit)
            val entities = entitiesAsc.reversed()
            val messages = entities.map { entity -> messageMapper.entityToDomain(entity) }

            // 동기화 확인 및 실행 (데이터 부족 시)
            checkAndSyncIfNeeded(
                channelId = channelIdParam,
                resultSize = messages.size,
                requestedLimit = limit,
                timestamp = afterTimestamp,
                loadType = "Prepend"
            )

            currentLoadedItems += messages.size
            if (messages.isNotEmpty()) consecutiveEmptyLoads = 0

            val oldestTimestamp = entities.lastOrNull()?.createdAt
            val newestTimestamp = entities.firstOrNull()?.createdAt

            val prevKey = if (entities.isEmpty() || entities.size < limit) {
                Log.d("MessagePagingSource", "⬆️ Prepend prevKey=null: 더 최신 데이터 없음 (size=${entities.size}, limit=$limit)")
                null
            } else {
                Log.d("MessagePagingSource", "⬆️ Prepend prevKey=$newestTimestamp: 더 최신 데이터 있음")
                newestTimestamp
            }
            val nextKey = if (entities.isEmpty()) null else oldestTimestamp

            Log.d(
                "MessagePagingSource",
                "⬆️ Prepend result: size=${messages.size}, oldest=$oldestTimestamp, newest=$newestTimestamp, prevKey=$prevKey, nextKey=$nextKey, totalLoaded=$currentLoadedItems"
            )

            return LoadResult.Page(
                data = messages,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }

        private suspend fun handleAppend(beforeTimestamp: Long?, limit: Int, channelIdParam: String): LoadResult<Long, Message> {
            Log.d(
                "MessagePagingSource",
                "⬇️ Append (과거 메시지 로딩): beforeTs=$beforeTimestamp, limit=$limit, channel=$channelIdParam"
            )

            val entities = messageDao.getMessagesBefore(channelIdParam, beforeTimestamp ?: Long.MAX_VALUE, limit)
            val messages = entities.map { entity -> messageMapper.entityToDomain(entity) }

            // 동기화 확인 및 실행 (데이터 부족 시)
            checkAndSyncIfNeeded(
                channelId = channelIdParam,
                resultSize = messages.size,
                requestedLimit = limit,
                timestamp = beforeTimestamp,
                loadType = "Append"
            )

            currentLoadedItems += messages.size
            if (messages.isNotEmpty()) consecutiveEmptyLoads = 0

            val oldestTimestamp = entities.lastOrNull()?.createdAt
            val newestTimestamp = entities.firstOrNull()?.createdAt

            val prevKey = if (entities.isEmpty()) null else newestTimestamp
            val nextKey = if (entities.isEmpty() || entities.size < limit) {
                Log.d("MessagePagingSource", "⬇️ Append nextKey=null: 더 과거 데이터 없음 (size=${entities.size}, limit=$limit)")
                null
            } else {
                Log.d("MessagePagingSource", "⬇️ Append nextKey=$oldestTimestamp: 더 과거 데이터 있음")
                oldestTimestamp
            }

            Log.d(
                "MessagePagingSource",
                "⬇️ Append result: size=${messages.size}, oldest=$oldestTimestamp, newest=$newestTimestamp, prevKey=$prevKey, nextKey=$nextKey, totalLoaded=$currentLoadedItems"
            )

            return LoadResult.Page(
                data = messages,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }

        override fun getRefreshKey(state: PagingState<Long, Message>): Long? {
            // 조건부 null: 앵커 아이템이 있으면 그 createdAt을 키로 반환, 없으면 null로 최신부터 로딩
            val anchorPosition = state.anchorPosition ?: return null
            val anchorItem = state.closestItemToPosition(anchorPosition)
            return anchorItem?.createdAt?.toEpochMilli()
        }

        // ================================
        // 동기화 확인 관련 헬퍼 메서드들
        // ================================

        /**
         * 서버에 더 많은 데이터가 있는지 확인
         */
        private suspend fun checkIfMoreDataAvailable(
            channelId: String,
            requestTimestamp: Long?
        ): Boolean {
            return try {
                // 커서 스토어에서 마지막 동기화 시점 확인
                val streamName = "messages-$channelId"
                val lastCursor = cursorStore.getCursor(streamName)
                val lastSyncTime = lastCursor?.toLongOrNull() ?: 0L
                val requestTime = requestTimestamp ?: System.currentTimeMillis()

                // 요청 시점이 마지막 동기화보다 이전이면 더 많은 데이터 있을 가능성
                val hasMoreData = requestTime < lastSyncTime

                Log.d("MessagePagingSource", "🔍 More data check for channel '$channelId':")
                Log.d("MessagePagingSource", "   Request timestamp: $requestTime")
                Log.d("MessagePagingSource", "   Last sync cursor: $lastCursor ($lastSyncTime)")
                Log.d("MessagePagingSource", "   Has more data: $hasMoreData")

                hasMoreData
            } catch (e: Exception) {
                Log.w("MessagePagingSource", "⚠️ Failed to check more data availability", e)
                // 확인 실패 시 안전하게 false 반환 (불필요한 동기화 방지)
                false
            }
        }

        /**
         * 쓰로틀링 확인 후 동기화 필요성 로깅만 수행
         * 실제 동기화는 UseCase 또는 ViewModel 레이어에서 수행
         */
        private suspend fun logSyncNeedIfThrottled(
            channelId: String,
            loadType: String,
            reason: String
        ): Boolean {
            return if (syncThrottler.canSync(channelId, loadType)) {
                Log.d(
                    "MessagePagingSource",
                    "🚀 Sync needed for '$channelId:$loadType' (reason: $reason)"
                )
                true
            } else {
                Log.d(
                    "MessagePagingSource",
                    "🔥 Sync throttled for '$channelId:$loadType' (reason: $reason)"
                )
                false
            }
        }

        /**
         * 데이터 부족 상황에서 동기화 필요성 확인
         */
        private suspend fun checkAndSyncIfNeeded(
            channelId: String,
            resultSize: Int,
            requestedLimit: Int,
            timestamp: Long?,
            loadType: String
        ): Boolean {
            // 데이터가 충분하면 동기화 불필요
            if (resultSize >= requestedLimit) {
                return false
            }

            Log.d("MessagePagingSource", "📊 Data shortage detected:")
            Log.d("MessagePagingSource", "   Channel: $channelId")
            Log.d("MessagePagingSource", "   Load type: $loadType")
            Log.d("MessagePagingSource", "   Result size: $resultSize / $requestedLimit")
            Log.d("MessagePagingSource", "   Timestamp: $timestamp")

            // 서버에 더 많은 데이터가 있는지 확인
            val hasMoreData = checkIfMoreDataAvailable(channelId, timestamp)
            if (!hasMoreData) {
                Log.d("MessagePagingSource", "📊 No more data available on server")
                return false
            }

            // 쓰로틀링 확인 후 동기화 필요성 로깅
            return logSyncNeedIfThrottled(
                channelId,
                loadType,
                "data shortage: $resultSize/$requestedLimit"
            )
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

    private fun Map<String, Any?>.toJsonString(): String {
        val jsonObject = buildJsonObject {
            this@toJsonString.forEach { (key, value) ->
                put(key, toJsonElement(value))
            }
        }
        return jsonObject.toString()
    }

    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value.toDouble())
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) ->
                    if (k != null) put(k.toString(), toJsonElement(v))
                }
            }
            is List<*> -> buildJsonArray {
                value.forEach { elem -> add(toJsonElement(elem)) }
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun buildOutboxPayload(channelId: String, payload: Map<String, Any?>): String {
        val merged: MutableMap<String, Any?> = LinkedHashMap(payload)
        // channelId는 항상 최종 페이로드에 포함되도록 보장
        merged["channelId"] = channelId
        return merged.toJsonString()
    }
}
