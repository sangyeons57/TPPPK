package com.example.data.cache

import android.util.Log
import com.example.core_common.config.FeatureFlags
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.local.LocalChatDataSource
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.domain.model.base.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 캐시 관리자 구현체
 * 로컬 캐시 우선 전략으로 즉시 응답하고 백그라운드에서 효율적인 증분 동기화 수행
 */
@Singleton
class ChatCacheManagerImpl @Inject constructor(
    private val localDataSource: LocalChatDataSource,
    private val remoteDataSource: MessageRemoteDataSource
) : ChatCacheManager {

    companion object {
        private const val TAG = "ChatCacheManager"
        private const val MAX_CACHE_SIZE = 500 // 채널당 최대 캐시 메시지 개수
    }

    /**
     * 스마트 동기화 조건 확인
     * WebSocket 상태와 시간 간격을 고려하여 동기화 필요성 판단
     */
    private fun shouldPerformSync(channelId: String, lastSyncTimestamp: Instant): Boolean {
        val now = Instant.now()
        val timeSinceLastSync = Duration.between(lastSyncTimestamp, now)

        // 조건 1: 처음 동기화인 경우 (항상 수행)
        if (lastSyncTimestamp == Instant.EPOCH) {
            Log.d(TAG, "Performing initial sync for channel: $channelId")
            return true
        }

        // 조건 2: 설정된 간격보다 오래된 경우 (안전망 동기화)
        val syncIntervalMinutes = FeatureFlags.CACHE_SYNC_INTERVAL_MINUTES
        if (timeSinceLastSync.toMinutes() >= syncIntervalMinutes) {
            Log.d(
                TAG,
                "Performing safety sync for channel $channelId: ${timeSinceLastSync.toMinutes()} minutes since last sync"
            )
            return true
        }

        // 조건 3: 최근 동기화한 경우 생략 (WebSocket이 실시간 처리하고 있을 것)
        Log.d(
            TAG,
            "Skipping sync for channel $channelId: only ${timeSinceLastSync.toMinutes()} minutes since last sync (WebSocket handling realtime)"
        )
        return false
    }

    // 백그라운드 동기화를 위한 CoroutineScope
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun getMessagesWithSync(channelId: String, limit: Int): List<Message> {
        Log.d(TAG, "Getting messages for channel: $channelId (limit: $limit)")

        // 1. 로컬 캐시에서 즉시 반환
        val cachedMessages = localDataSource.getMessages(channelId, limit)
        Log.d(TAG, "Found ${cachedMessages.size} cached messages for channel: $channelId")

        // 2. 백그라운드에서 동기화 시작 (사용자 대기하지 않음)
        backgroundScope.launch {
            try {
                val syncResult = syncChannelIncremental(channelId)
                if (syncResult.success && syncResult.newMessageCount > 0) {
                    Log.d(
                        TAG,
                        "Background sync completed: ${syncResult.newMessageCount} new messages for channel: $channelId"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background sync failed for channel: $channelId", e)
            }
        }

        return cachedMessages
    }

    override suspend fun syncChannelIncremental(channelId: String, forceSync: Boolean): SyncResult {
        Log.i(TAG, "SYNC_START: Incremental sync for channel: $channelId (forceSync: $forceSync)")

        return try {
            val lastSyncTimestamp = localDataSource.getLastSyncTimestamp(channelId) ?: Instant.EPOCH
            Log.i(TAG, "SYNC_INFO: Channel $channelId | Last Sync: $lastSyncTimestamp")

            if (!forceSync && !shouldPerformSync(channelId, lastSyncTimestamp)) {
                Log.i(TAG, "SYNC_SKIP: Conditions not met for channel $channelId.")
                return SyncResult.success(newCount = 0)
            }

            Log.d(
                TAG,
                "SYNC_FETCH: Fetching messages from Firestore for channel $channelId since $lastSyncTimestamp"
            )
            when (val result =
                remoteDataSource.getMessagesAfterTimestamp(channelId, lastSyncTimestamp)) {
                is CustomResult.Success -> {
                    val firestoreMessages = result.data
                    Log.i(
                        TAG,
                        "SYNC_FETCH_SUCCESS: Fetched ${firestoreMessages.size} messages from Firestore for channel: $channelId"
                    )

                    if (firestoreMessages.isEmpty()) {
                        Log.d(
                            TAG,
                            "SYNC_NO_CHANGES: No new messages from Firestore. Sync complete."
                        )
                        localDataSource.updateSyncTimestamp(channelId, DateTimeUtil.nowInstant())
                        return SyncResult.success(newCount = 0)
                    }

                    // Firestore에서 가져온 데이터 상세 로깅
                    firestoreMessages.forEach { msg ->
                        Log.d(
                            TAG, "SYNC_FIRESTORE_DATA: msgId=${msg.id.value}, " +
                                    "createdAt=${msg.createdAt}, updatedAt=${msg.updatedAt}, " +
                                    "isDeleted=${msg.isDeleted.value}, content='${
                                        msg.content.value.take(
                                            20
                                        )
                                    }...'"
                        )
                    }

                    // 로컬 캐시와 비교 로깅 (옵션: 성능에 영향 줄 수 있으므로 필요한 경우에만 활성화)
                    val localMessages =
                        localDataSource.getMessagesAfter(channelId, lastSyncTimestamp)
                    Log.d(
                        TAG,
                        "SYNC_LOCAL_DATA: Found ${localMessages.size} messages in local cache for the same period."
                    )

                    // 변경점 비교
                    val firestoreIds = firestoreMessages.map { it.id.value }.toSet()
                    val localIds = localMessages.map { it.id.value }.toSet()
                    val newIds = firestoreIds - localIds
                    val potentiallyModifiedIds = firestoreIds.intersect(localIds)

                    Log.i(
                        TAG,
                        "SYNC_COMPARE: New: ${newIds.size}, Potentially Modified: ${potentiallyModifiedIds.size}"
                    )
                    if (newIds.isNotEmpty()) {
                        Log.d(TAG, "SYNC_COMPARE_NEW_IDS: ${newIds.joinToString()}")
                    }

                    Log.i(
                        TAG,
                        "SYNC_SAVE_START: Saving ${firestoreMessages.size} messages to local cache for channel: $channelId"
                    )
                    localDataSource.saveMessages(channelId, firestoreMessages)
                    Log.i(TAG, "SYNC_SAVE_COMPLETE: Successfully saved messages to local cache.")

                    manageCache(channelId)

                    val newTimestamp = DateTimeUtil.nowInstant()
                    localDataSource.updateSyncTimestamp(channelId, newTimestamp)
                    Log.i(
                        TAG,
                        "SYNC_TIMESTAMP_UPDATE: Updated sync timestamp for channel $channelId to $newTimestamp"
                    )

                    SyncResult.success(newCount = firestoreMessages.size)
                }

                is CustomResult.Failure -> {
                    Log.e(
                        TAG,
                        "SYNC_FETCH_FAILURE: Firestore fetch failed for channel: $channelId",
                        result.error
                    )
                    SyncResult.failure("Firestore sync failed: ${result.error.message}")
                }

                else -> {
                    Log.w(TAG, "SYNC_FETCH_UNKNOWN: Unknown result state for channel: $channelId")
                    SyncResult.failure("Sync in progress or unknown state")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SYNC_ERROR: Unhandled exception during sync for channel: $channelId", e)
            SyncResult.failure("Sync error: ${e.message}")
        }
    }

    override suspend fun syncChannelFull(channelId: String, initialLimit: Int): SyncResult {
        Log.d(TAG, "Starting full sync for channel: $channelId (limit: $initialLimit)")

        return try {
            // Firestore에서 최신 메시지들 가져오기
            when (val result = remoteDataSource.getRecentMessages(channelId, initialLimit)) {
                is CustomResult.Success -> {
                    val messages = result.data
                    Log.d(
                        TAG,
                        "Fetched ${messages.size} messages from Firestore for full sync: $channelId"
                    )

                    // 기존 캐시 삭제 후 새로운 메시지들로 대체
                    localDataSource.clearChannel(channelId)

                    if (messages.isNotEmpty()) {
                        localDataSource.saveMessages(channelId, messages)
                        Log.d(
                            TAG,
                            "Replaced cache with ${messages.size} messages for channel: $channelId"
                        )
                    }

                    // 동기화 시간 업데이트
                    localDataSource.updateSyncTimestamp(channelId, Instant.now())

                    SyncResult.success(newCount = messages.size)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Failed to fetch messages for full sync: $channelId", result.error)
                    SyncResult.failure("Full sync failed: ${result.error.message}")
                }

                else -> {
                    Log.d(TAG, "Full sync in progress for channel: $channelId")
                    SyncResult.failure("Full sync in progress")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Full sync error for channel: $channelId", e)
            SyncResult.failure("Full sync error: ${e.message}")
        }
    }

    override fun observeChannelMessages(channelId: String, limit: Int): Flow<List<Message>> {
        Log.d(TAG, "Observing messages for channel: $channelId (limit: $limit)")
        return localDataSource.observeMessages(channelId, limit)
    }

    override suspend fun addRealtimeMessage(channelId: String, message: Message) {
        Log.d(TAG, "Adding realtime message to cache: ${message.id.value} for channel: $channelId")

        try {
            // 강화된 중복 방지 로직
            if (localDataSource.messageExists(message.id.value)) {
                Log.d(TAG, "Message already exists in cache, skipping: ${message.id.value}")
                return
            }

            // 클라이언트 주도 ID를 사용하여 로컬 캐시에 저장 (upsert 방식)
            // OnConflictStrategy.REPLACE로 동일 ID 메시지 상태만 업데이트
            localDataSource.saveMessage(channelId, message)
            Log.d(TAG, "Successfully added/updated realtime message in cache: ${message.id.value}")

            // 캐시 크기 관리
            manageCache(channelId)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add realtime message to cache: ${message.id.value}", e)
        }
    }

    override suspend fun updateRealtimeMessage(channelId: String, message: Message) {
        Log.d(
            TAG,
            "Updating realtime message in cache: ${message.id.value} for channel: $channelId (deleted: ${message.isDeleted.value})"
        )

        try {
            // 기존 메시지가 캐시에 있는지 확인
            if (!localDataSource.messageExists(message.id.value)) {
                Log.w(TAG, "Message not found in cache for update: ${message.id.value}")
                // 메시지가 없어도 upsert로 추가 (나중에 동기화에서 처리될 수 있음)
            }

            // 로컬 캐시에 upsert (REPLACE 전략으로 자동 업데이트)
            localDataSource.saveMessage(channelId, message)
            Log.d(TAG, "Successfully updated realtime message in cache: ${message.id.value}")

            // 삭제된 메시지는 캐시 크기 관리에서 정리됨

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update realtime message in cache: ${message.id.value}", e)
        }
    }

    override suspend fun loadMoreMessages(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int
    ): List<Message> {
        Log.d(
            TAG,
            "Loading more messages for channel: $channelId before: $beforeTimestamp (limit: $limit)"
        )

        return try {
            // 먼저 로컬 캐시에서 확인
            val cachedMessages =
                localDataSource.getMessagesBefore(channelId, beforeTimestamp, limit)

            if (cachedMessages.size >= limit) {
                Log.d(TAG, "Found sufficient cached messages: ${cachedMessages.size}")
                return cachedMessages
            }

            // 로컬 캐시에 충분하지 않으면 Firestore에서 가져오기
            when (val result =
                remoteDataSource.getMessagesBeforeTimestamp(channelId, beforeTimestamp, limit)) {
                is CustomResult.Success -> {
                    val remoteMessages = result.data
                    Log.d(TAG, "Fetched ${remoteMessages.size} older messages from Firestore")

                    if (remoteMessages.isNotEmpty()) {
                        // 로컬 캐시에 저장
                        localDataSource.saveMessages(channelId, remoteMessages)

                        // 캐시 크기 관리
                        manageCache(channelId)
                    }

                    // 로컬 캐시와 원격 메시지 합치기 (중복 제거)
                    val allMessages = (cachedMessages + remoteMessages)
                        .distinctBy { it.id.value }
                        .sortedByDescending { it.createdAt }
                        .take(limit)

                    Log.d(TAG, "Returning ${allMessages.size} total messages for pagination")
                    allMessages
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Failed to fetch older messages from Firestore", result.error)
                    cachedMessages // 실패 시 캐시된 메시지라도 반환
                }

                else -> {
                    Log.d(TAG, "Firestore fetch in progress, returning cached messages")
                    cachedMessages
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more messages for channel: $channelId", e)
            emptyList()
        }
    }

    override suspend fun getCacheStatus(channelId: String): CacheStatus {
        val syncInfo = localDataSource.getSyncInfo(channelId)
        val now = Instant.now()

        return if (syncInfo != null) {
            val timeSinceLastSync = Duration.between(syncInfo.lastSyncTimestamp, now)

            CacheStatus(
                channelId = channelId,
                messageCount = syncInfo.messageCount,
                lastSyncTimestamp = syncInfo.lastSyncTimestamp,
                oldestMessageTimestamp = syncInfo.oldestMessageTimestamp,
                newestMessageTimestamp = syncInfo.newestMessageTimestamp,
                hasMoreOlderMessages = syncInfo.hasMoreOlderMessages,
                syncFailureCount = syncInfo.syncFailureCount,
                isStale = timeSinceLastSync.toMinutes() >= FeatureFlags.CACHE_SYNC_INTERVAL_MINUTES
            )
        } else {
            CacheStatus(
                channelId = channelId,
                messageCount = 0,
                lastSyncTimestamp = null,
                oldestMessageTimestamp = null,
                newestMessageTimestamp = null,
                hasMoreOlderMessages = true,
                syncFailureCount = 0,
                isStale = true
            )
        }
    }

    override suspend fun clearChannelCache(channelId: String) {
        Log.d(TAG, "Clearing cache for channel: $channelId")
        localDataSource.clearChannel(channelId)
    }

    override suspend fun clearAllCache() {
        Log.d(TAG, "Clearing ALL chat cache and sync info (전체 삭제)")
        localDataSource.clearAll()
    }

    override suspend fun syncAfterWebSocketRecovery(channelId: String): SyncResult {
        Log.d(TAG, "Performing WebSocket recovery sync for channel: $channelId")

        // 강제 동기화 수행 (시간 간격 무시)
        return syncChannelIncremental(channelId, forceSync = true)
    }

    /**
     * 채널의 캐시 크기를 관리 (오래된 메시지 삭제)
     * @param channelId 채널 ID
     */
    private suspend fun manageCache(channelId: String) {
        try {
            val deletedCount = localDataSource.deleteOldMessages(channelId, MAX_CACHE_SIZE)
            if (deletedCount > 0) {
                Log.d(TAG, "Deleted $deletedCount old messages from cache for channel: $channelId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to manage cache for channel: $channelId", e)
        }
    }
}
