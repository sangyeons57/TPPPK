package com.example.domain.repository.base

import com.example.domain.model.base.Message
import com.example.domain.repository.Repository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 채팅 캐시 관리 레포지토리 인터페이스
 * 로컬 캐시와 Firestore 간의 효율적인 동기화를 제공
 */
interface ChatCacheRepository : Repository {

    /**
     * 채널의 메시지를 로컬 캐시에서 즉시 반환하고 백그라운드에서 동기화 수행
     */
    suspend fun getMessagesWithSync(limit: Int = 50): List<Message>

    /**
     * 채널 메시지를 실시간으로 관찰
     */
    fun observeChannelMessages(limit: Int = 50): Flow<List<Message>>

    /**
     * 과거 메시지를 추가로 로딩 (페이지네이션)
     */
    suspend fun loadMoreMessages(
        beforeTimestamp: Instant,
        limit: Int = 50
    ): List<Message>

    /**
     * 실시간 메시지를 로컬 캐시에 추가
     */
    suspend fun addRealtimeMessage(message: Message)

    /**
     * 실시간 메시지 업데이트/삭제를 캐시에 반영
     */
    suspend fun updateRealtimeMessage(message: Message)

    /**
     * 채널의 증분 동기화 수행
     */
    suspend fun syncChannelIncremental(forceSync: Boolean = false)

    /**
     * 채널의 전체 동기화 수행
     */
    suspend fun syncChannelFull(initialLimit: Int = 50)

    /**
     * 채널의 로컬 캐시 초기화
     */
    suspend fun clearChannelCache()
}