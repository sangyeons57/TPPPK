package com.example.data.datasource.local

import com.example.domain.model.base.Message
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 채팅 데이터 저장소 인터페이스
 * updateAt 기반 증분 동기화와 효율적인 캐싱을 지원
 */
interface LocalChatDataSource {

    /**
     * 채널의 최신 메시지들을 가져옴 (초기 로딩용)
     * @param channelId 채널 ID
     * @param limit 가져올 메시지 개수
     * @return 최신 메시지 목록 (생성시간 내림차순)
     */
    suspend fun getMessages(channelId: String, limit: Int): List<Message>

    /**
     * 특정 시점 이후 업데이트된 메시지들을 가져옴 (증분 동기화용)
     * 🔑 핵심: updateAt > timestamp 조건으로 변경된 메시지만 효율적으로 가져옴
     * @param channelId 채널 ID
     * @param timestamp 마지막 동기화 시간
     * @return 업데이트된 메시지 목록
     */
    suspend fun getMessagesAfter(channelId: String, timestamp: Instant): List<Message>

    /**
     * 특정 시점 이전의 과거 메시지들을 가져옴 (페이지네이션용)
     * @param channelId 채널 ID
     * @param beforeTimestamp 기준 시간
     * @param limit 가져올 메시지 개수
     * @return 과거 메시지 목록
     */
    suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int
    ): List<Message>

    /**
     * 메시지들을 배치로 저장
     * @param channelId 채널 ID
     * @param messages 저장할 메시지들
     */
    suspend fun saveMessages(channelId: String, messages: List<Message>)

    /**
     * 단일 메시지 저장 (실시간 메시지용)
     * @param channelId 채널 ID
     * @param message 저장할 메시지
     */
    suspend fun saveMessage(channelId: String, message: Message)

    /**
     * 채널의 메시지들을 실시간으로 관찰
     * @param channelId 채널 ID
     * @param limit 최대 메시지 개수
     * @return 메시지 Flow
     */
    fun observeMessages(channelId: String, limit: Int): Flow<List<Message>>

    /**
     * 채널의 마지막 동기화 시간 조회
     * @param channelId 채널 ID
     * @return 마지막 동기화 시간 (없으면 null)
     */
    suspend fun getLastSyncTimestamp(channelId: String): Instant?

    /**
     * 채널의 동기화 시간 업데이트
     * @param channelId 채널 ID
     * @param timestamp 새로운 동기화 시간
     */
    suspend fun updateSyncTimestamp(channelId: String, timestamp: Instant)

    /**
     * 채널의 동기화 상태 정보 조회
     * @param channelId 채널 ID
     * @return 동기화 상태 정보
     */
    suspend fun getSyncInfo(channelId: String): ChatSyncInfo?

    /**
     * 채널의 동기화 상태 정보 업데이트
     * @param channelId 채널 ID
     * @param syncInfo 새로운 동기화 상태 정보
     */
    suspend fun updateSyncInfo(channelId: String, syncInfo: ChatSyncInfo)

    /**
     * 오래된 메시지들을 삭제 (메모리 관리용)
     * @param channelId 채널 ID
     * @param keepCount 유지할 메시지 개수
     * @return 삭제된 메시지 개수
     */
    suspend fun deleteOldMessages(channelId: String, keepCount: Int): Int

    /**
     * 특정 메시지 존재 여부 확인
     * @param messageId 메시지 ID
     * @return 존재 여부
     */
    suspend fun messageExists(messageId: String): Boolean

    /**
     * 채널의 모든 메시지 삭제
     * @param channelId 채널 ID
     */
    suspend fun clearChannel(channelId: String)
}

/**
 * 채널 동기화 상태 정보
 */
data class ChatSyncInfo(
    val channelId: String,
    val lastSyncTimestamp: Instant,
    val messageCount: Int,
    val hasMoreOlderMessages: Boolean,
    val oldestMessageTimestamp: Instant?,
    val newestMessageTimestamp: Instant?,
    val syncFailureCount: Int = 0
)