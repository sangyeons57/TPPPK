package com.example.domain_repository.base

import androidx.paging.PagingSource
import com.example.core_common.result.CustomResult
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.vo.DocumentId
import com.example.domain_repository.DefaultRepository

// 메시지 전송 시 사용할 첨부파일 모델 (도메인 모델 MessageAttachment와 구분)
data class MessageAttachmentToSend(
    val fileName: String,
    val mimeType: String,
    val sourceUri: String // 예시: content URI 또는 file URI
    // val bytes: ByteArray? // 또는 직접 바이트를 전달할 경우
)

/**
 * 통합된 메시지 Repository 인터페이스
 * 로컬(Room) + 리모트(Firestore) SSOT 패턴 구현
 */
interface MessageRepository : DefaultRepository<Message> {

    // ================================
    // 기존 메시지 관리 기능
    // ================================

    suspend fun sendMessage(channelId: String, content: String): String
    suspend fun deleteMessage(id: String)

    // ================================
    // Paging3 지원 기능
    // ================================

    /**
     * 메시지용 PagingSource 제공 (시간 역순)
     * @return 타임스탬프 키를 사용하는 PagingSource
     */
    fun getMessagesPagingSource(): PagingSource<Long, Message>

    /**
     * 특정 채널의 메시지용 PagingSource 제공 (시간 역순)
     * @param channelId 채널 ID
     * @return 해당 채널의 메시지들만 포함하는 PagingSource
     */
    fun getMessagesPagingSource(channelId: String): PagingSource<Long, Message>

    // ================================
    // 시간 기반 메시지 조회 (Paging 지원)
    // ================================

    /**
     * 특정 시간 이후의 메시지들 조회
     * @param channelId 채널 ID
     * @param afterTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이후의 메시지 목록
     */
    suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Long,
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>

    /**
     * 특정 시간 이전의 메시지들 조회
     * @param channelId 채널 ID
     * @param beforeTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이전의 메시지 목록
     */
    suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Long,
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>

    /**
     * 특정 시간 범위의 메시지들 조회
     * @param channelId 채널 ID
     * @param startTimestamp 시작 시간 (epoch milliseconds)
     * @param endTimestamp 종료 시간 (epoch milliseconds)
     * @return 해당 시간 범위의 메시지 목록
     */
    suspend fun getMessagesBetween(
        channelId: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): CustomResult<List<Message>, Exception>

    // ================================
    // OutBox 기반 동기화 상태 관리
    // ================================

    /**
     * OutBox 상태를 기반으로 메시지의 동기화 상태 조회
     * @param messageId 메시지 ID
     * @return 동기화 상태
     */
    suspend fun getMessageOutBoxStatus(messageId: DocumentId): CustomResult<OutBoxStatus, Exception>

    /**
     * 메시지 ACK 처리 (WebSocket ACK 수신 시)
     * @param messageId 메시지 ID
     * @return 처리 결과
     */
    suspend fun handleMessageAck(messageId: String): CustomResult<Unit, Exception>

    /**
     * 메시지 실패 처리 (WebSocket FAILED 수신 시)
     * @param messageId 메시지 ID
     * @return 처리 결과
     */
    suspend fun handleMessageFailure(messageId: String): CustomResult<Unit, Exception>

    /**
     * 특정 채널의 동기화 상태별 메시지 개수 조회
     * @param channelId 채널 ID
     * @return 동기화 상태별 메시지 개수 맵
     */
    suspend fun getChannelOutBoxStatusCounts(channelId: String): CustomResult<Map<OutBoxStatus, Int>, Exception>

    // ================================
    // 캐시 관리 기능
    // ================================

    /**
     * 특정 채널의 로컬 캐시를 완전히 클리어
     * 메시지, OutBox, 동기화 메타데이터 모두 삭제
     * @param channelId 클리어할 채널 ID
     * @return 성공/실패 결과
     */
    suspend fun clearLocalCache(channelId: String): CustomResult<Unit, Exception>

    /**
     * 모든 메시지 관련 로컬 캐시를 완전히 클리어
     * 메시지, OutBox, 동기화 메타데이터 모두 삭제
     * @return 성공/실패 결과
     */
    suspend fun clearAllCache(): CustomResult<Unit, Exception>

    /**
     * 채널의 최신 메시지들을 Room DB에서 가져옴 (로컬 캐시)
     * @param channelId 채널 ID
     * @param limit 가져올 메시지 개수
     * @return 최신 메시지 목록
     */
    suspend fun getRecentMessages(
        channelId: String,
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>

    /**
     * 메시지 ID로 단일 메시지 조회
     * @param messageId 메시지 ID
     * @return 메시지 객체 (없으면 null)
     */
    suspend fun findById(messageId: String): Message?
}
