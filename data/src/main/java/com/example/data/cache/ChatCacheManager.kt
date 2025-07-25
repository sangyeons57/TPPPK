package com.example.data.cache

import com.example.domain.model.base.Message
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 채팅 캐시 관리자 인터페이스
 * 로컬 캐시와 Firestore 간의 효율적인 동기화를 제공
 */
interface ChatCacheManager {

    /**
     * 채널의 메시지를 로컬 캐시에서 즉시 반환하고 백그라운드에서 동기화 수행
     * 🔑 핵심: 사용자에게 즉시 응답하면서 최신 상태로 유지
     * @param channelId 채널 ID
     * @param limit 반환할 메시지 개수
     * @return 로컬 캐시된 메시지 목록
     */
    suspend fun getMessagesWithSync(channelId: String, limit: Int = 50): List<Message>

    /**
     * 채널의 증분 동기화 수행 (updateAt 기반)
     * 마지막 동기화 이후 변경된 메시지만 효율적으로 가져옴
     * @param channelId 채널 ID
     * @param forceSync 강제 동기화 여부 (WebSocket 복구 시 등)
     * @return 동기화 결과
     */
    suspend fun syncChannelIncremental(channelId: String, forceSync: Boolean = false): SyncResult

    /**
     * 채널의 전체 동기화 수행 (초기 설정용)
     * @param channelId 채널 ID
     * @param initialLimit 초기 로딩할 메시지 개수
     * @return 동기화 결과
     */
    suspend fun syncChannelFull(channelId: String, initialLimit: Int = 50): SyncResult

    /**
     * 채널 메시지를 실시간으로 관찰
     * @param channelId 채널 ID
     * @param limit 최대 메시지 개수
     * @return 메시지 Flow
     */
    fun observeChannelMessages(channelId: String, limit: Int = 50): Flow<List<Message>>

    /**
     * 실시간 메시지를 로컬 캐시에 추가 (WebSocket으로 받은 새 메시지)
     * @param channelId 채널 ID
     * @param message 추가할 메시지
     */
    suspend fun addRealtimeMessage(channelId: String, message: Message)

    /**
     * 실시간 메시지 업데이트/삭제를 캐시에 반영 (WebSocket으로 받은 수정/삭제 이벤트)
     * @param channelId 채널 ID
     * @param message 수정/삭제된 메시지 (isDeleted, updatedAt 등이 업데이트됨)
     */
    suspend fun updateRealtimeMessage(channelId: String, message: Message)

    /**
     * 과거 메시지를 추가로 로딩 (페이지네이션)
     * @param channelId 채널 ID
     * @param beforeTimestamp 기준 시간
     * @param limit 로딩할 메시지 개수
     * @return 로딩된 메시지 목록
     */
    suspend fun loadMoreMessages(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int = 50
    ): List<Message>

    /**
     * 채널의 로컬 캐시 상태 정보 조회
     * @param channelId 채널 ID
     * @return 캐시 상태 정보
     */
    suspend fun getCacheStatus(channelId: String): CacheStatus

    /**
     * 채널의 로컬 캐시 초기화
     * @param channelId 채널 ID
     */
    suspend fun clearChannelCache(channelId: String)

    /**
     * 모든 채널의 로컬 채팅 캐시 전체 삭제 (앱 데이터 초기화)
     */
    suspend fun clearAllCache()


    /**
     * WebSocket 연결 복구 시 동기화
     * 연결이 끊어진 동안 놓친 메시지들을 즉시 동기화
     * @param channelId 채널 ID
     * @return 동기화 결과
     */
    suspend fun syncAfterWebSocketRecovery(channelId: String): SyncResult
}

/**
 * 동기화 결과 정보
 */
data class SyncResult(
    val success: Boolean,
    val newMessageCount: Int = 0,
    val updatedMessageCount: Int = 0,
    val deletedMessageCount: Int = 0,
    val error: String? = null,
    val syncTimestamp: Instant = Instant.now()
) {
    companion object {
        fun success(newCount: Int = 0, updatedCount: Int = 0, deletedCount: Int = 0) = SyncResult(
            success = true,
            newMessageCount = newCount,
            updatedMessageCount = updatedCount,
            deletedMessageCount = deletedCount
        )

        fun failure(error: String) = SyncResult(
            success = false,
            error = error
        )
    }
}

/**
 * 캐시 상태 정보
 */
data class CacheStatus(
    val channelId: String,
    val messageCount: Int,
    val lastSyncTimestamp: Instant?,
    val oldestMessageTimestamp: Instant?,
    val newestMessageTimestamp: Instant?,
    val hasMoreOlderMessages: Boolean,
    val syncFailureCount: Int,
    val isStale: Boolean // 동기화가 필요한 상태인지
)