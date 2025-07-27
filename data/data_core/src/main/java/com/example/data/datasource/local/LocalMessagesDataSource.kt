package com.example.data.datasource.local

import com.example.domain.model.base.Message
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 메시지 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalMessagesDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 메시지 ID로 단일 메시지 조회
     * @param messageId 메시지 ID
     * @return 메시지 정보 (없으면 null)
     */
    suspend fun getMessageById(messageId: String): Message?

    /**
     * 채널별 메시지 목록 조회 (생성 시간 기준 정렬)
     * @param channelId 채널 ID
     * @param limit 조회할 메시지 수 (선택적)
     * @return 메시지 목록
     */
    suspend fun getMessagesByChannel(channelId: String, limit: Int? = null): List<Message>

    /**
     * 메시지 타입별 조회
     * @param messageType 메시지 타입 (TEXT, IMAGE, FILE, SYSTEM)
     * @return 메시지 목록
     */
    suspend fun getMessagesByType(messageType: String): List<Message>

    /**
     * 사용자별 메시지 목록 조회
     * @param userId 작성자 ID
     * @return 메시지 목록
     */
    suspend fun getMessagesByUser(userId: String): List<Message>

    /**
     * 메시지 검색 (내용 기준)
     * @param query 검색 쿼리
     * @param channelId 특정 채널 내 검색 (선택적)
     * @return 검색된 메시지 목록
     */
    suspend fun searchMessages(query: String, channelId: String? = null): List<Message>

    /**
     * 특정 시간 범위의 메시지 조회
     * @param channelId 채널 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메시지 목록
     */
    suspend fun getMessagesByTimeRange(
        channelId: String,
        startTime: Instant,
        endTime: Instant
    ): List<Message>

    /**
     * 단일 메시지 저장
     * @param message 저장할 메시지
     */
    suspend fun saveMessage(message: Message)

    /**
     * 메시지 목록 배치 저장
     * @param messages 저장할 메시지 목록
     */
    suspend fun saveMessages(messages: List<Message>)

    /**
     * 메시지 삭제
     * @param messageId 삭제할 메시지 ID
     */
    suspend fun deleteMessage(messageId: String)

    /**
     * 채널별 메시지 일괄 삭제
     * @param channelId 채널 ID
     */
    suspend fun deleteMessagesByChannel(channelId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 메시지들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @param channelId 특정 채널 (선택적)
     * @return 업데이트된 메시지 목록
     */
    suspend fun getMessagesUpdatedAfter(
        timestamp: Instant,
        channelId: String? = null
    ): List<Message>

    /**
     * 채널별 메시지 실시간 관찰
     * @param channelId 채널 ID
     * @return 메시지 목록 Flow
     */
    fun observeMessagesByChannel(channelId: String): Flow<List<Message>>

    /**
     * 특정 메시지 실시간 관찰
     * @param messageId 메시지 ID
     * @return 메시지 정보 Flow
     */
    fun observeMessageById(messageId: String): Flow<Message?>

    // === Outbox 관리 ===

    /**
     * 메시지 변경사항을 Outbox에 기록
     * @param messageId 메시지 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(messageId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<MessageOutboxOperation>

    /**
     * Outbox 작업 완료 처리
     * @param operationId 작업 ID
     */
    suspend fun markOutboxOperationComplete(operationId: String)

    /**
     * Outbox 작업 재시도 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementOutboxRetries(operationId: String)

    // === 동기화 메타데이터 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * @param channelId 특정 채널의 커서 (선택적)
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(channelId: String? = null): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     * @param channelId 특정 채널 (선택적)
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long, channelId: String? = null)

    // === 유틸리티 ===

    /**
     * 메시지 존재 여부 확인
     * @param messageId 메시지 ID
     * @return 존재 여부
     */
    suspend fun messageExists(messageId: String): Boolean

    /**
     * 채널별 메시지 수 조회
     * @param channelId 채널 ID
     * @return 메시지 수
     */
    suspend fun getMessageCount(channelId: String): Int

    /**
     * 전체 메시지 수 조회
     * @return 전체 메시지 수
     */
    suspend fun getTotalMessageCount(): Int

    /**
     * 채널의 마지막 메시지 조회
     * @param channelId 채널 ID
     * @return 마지막 메시지 (없으면 null)
     */
    suspend fun getLastMessage(channelId: String): Message?

    /**
     * 읽지 않은 메시지 수 조회
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @param lastReadTimestamp 마지막 읽은 시간
     * @return 읽지 않은 메시지 수
     */
    suspend fun getUnreadMessageCount(
        channelId: String,
        userId: String,
        lastReadTimestamp: Instant
    ): Int

    /**
     * 모든 메시지 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllMessages()
}

/**
 * 메시지 Outbox 작업 정보
 */
data class MessageOutboxOperation(
    val id: String,
    val messageId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)