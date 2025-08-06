package com.example.domain_repository.local

import androidx.paging.PagingSource
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId

/**
 * Message 로컬 저장소 Repository 인터페이스
 * Message 도메인 특화 로컬 저장소 작업 정의
 * Paging3 지원을 포함한 통합 인터페이스
 */
interface LocalMessageRepository : BaseLocalRepository<Message> {

    // ================================
    // Paging3 지원 메서드
    // ================================

    /**
     * 메시지용 PagingSource 제공 (시간 역순)
     * @return 타임스탬프 키를 사용하는 PagingSource
     */
    fun getMessagesPagingSource(): PagingSource<Long, Message>

    // ================================
    // Message 도메인 특화 조회 작업
    // ================================
    
    /**
     * 특정 시간 이후의 메시지들 조회 (페이징용)
     * @param afterTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이후의 메시지 목록
     */
    suspend fun getMessagesAfter(
        afterTimestamp: Long, 
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>
    
    /**
     * 특정 시간 이전의 메시지들 조회 (페이징용)
     * @param beforeTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이전의 메시지 목록
     */
    suspend fun getMessagesBefore(
        beforeTimestamp: Long, 
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>
    
    /**
     * 특정 시간 범위의 메시지들 조회
     * @param startTimestamp 시작 시간 (epoch milliseconds)
     * @param endTimestamp 종료 시간 (epoch milliseconds)
     * @return 해당 시간 범위의 메시지 목록
     */
    suspend fun getMessagesBetween(
        startTimestamp: Long, 
        endTimestamp: Long
    ): CustomResult<List<Message>, Exception>

    // ================================
    // 사용자 기반 조회 작업
    // ================================
    
    /**
     * 특정 사용자가 보낸 메시지들 조회
     * @param senderId 발신자 ID
     * @return 해당 사용자의 메시지 목록
     */
    suspend fun getMessagesBySender(senderId: UserId): CustomResult<List<Message>, Exception>
    
    /**
     * 답글 메시지들 조회
     * @param replyToMessageId 원본 메시지 ID
     * @return 해당 메시지에 대한 답글 목록
     */
    suspend fun getRepliesByMessageId(replyToMessageId: DocumentId): CustomResult<List<Message>, Exception>

    // ================================
    // 동기화 관련 작업
    // ================================
    
    /**
     * 특정 동기화 상태의 메시지들 조회
     * @param syncStatus 동기화 상태
     * @return 해당 상태의 메시지 목록
     */
    suspend fun getMessagesBySyncStatus(syncStatus: SyncStatus): CustomResult<List<Message>, Exception>
    
    /**
     * 동기화가 필요한 메시지들 조회
     * @return 동기화가 필요한 메시지 목록
     */
    suspend fun getUnsyncedMessages(): CustomResult<List<Message>, Exception>
    
    /**
     * 에러 상태의 메시지들 조회
     * @return 동기화 에러가 발생한 메시지 목록
     */
    suspend fun getErrorMessages(): CustomResult<List<Message>, Exception>
    
    /**
     * 특정 서버 버전 이후의 메시지들 조회
     * @param version 기준 서버 버전
     * @return 해당 버전 이후의 메시지 목록
     */
    suspend fun getMessagesAfterVersion(version: Long): CustomResult<List<Message>, Exception>
    
    /**
     * Message의 동기화 상태 업데이트
     * @param messageId 메시지 ID
     * @param syncStatus 새로운 동기화 상태
     * @param serverVersion 서버 버전 (선택사항)
     * @param serverUpdatedAt 서버 업데이트 시간 (선택사항)
     * @return 성공/실패 결과
     */
    suspend fun updateSyncStatus(
        messageId: DocumentId,
        syncStatus: SyncStatus,
        serverVersion: Long? = null,
        serverUpdatedAt: Long? = null
    ): CustomResult<Unit, Exception>

    /**
     * Message의 전송 상태 업데이트
     * @param messageId 메시지 ID
     * @param deliveryStatus 새로운 전송 상태 (SENDING/SENT/FAILED)
     * @return 성공/실패 결과
     */
    suspend fun updateDeliveryStatus(
        messageId: DocumentId,
        deliveryStatus: String
    ): CustomResult<Unit, Exception>

    /**
     * Message 저장 시 전송 상태 지정
     * @param message 저장할 메시지
     * @param deliveryStatus 전송 상태 (SENDING/SENT/FAILED)
     * @return 성공/실패 결과
     */
    suspend fun saveWithDeliveryStatus(
        message: Message,
        deliveryStatus: String
    ): CustomResult<DocumentId, Exception>

    // ================================
    // 배치 처리 작업
    // ================================
    
    /**
     * 동기화가 필요한 메시지들을 제한된 개수만큼 조회 (배치 처리용)
     * @param limit 조회할 최대 개수
     * @return 생성시간 순으로 정렬된 미동기화 메시지 목록
     */
    suspend fun getUnsyncedMessagesWithLimit(limit: Int): CustomResult<List<Message>, Exception>
    
    /**
     * 여러 메시지들의 동기화 상태를 일괄 업데이트
     * @param messageIds 업데이트할 메시지 ID 목록
     * @param newSyncStatus 새로운 동기화 상태
     * @return 업데이트된 메시지 개수
     */
    suspend fun updateSyncStatusByIds(
        messageIds: List<DocumentId>, 
        newSyncStatus: SyncStatus
    ): CustomResult<Int, Exception>

    // ================================
    // 정리 및 관리 작업
    // ================================
    
    /**
     * 오래된 메시지들 정리 (특정 시간 이전)
     * @param beforeTimestamp 기준 시간 이전 (epoch milliseconds)
     * @return 삭제된 메시지 개수
     */
    suspend fun deleteOldMessages(beforeTimestamp: Long): CustomResult<Int, Exception>

    // ================================
    // 통계 및 개수 조회
    // ================================
    
    /**
     * 동기화가 필요한 메시지 개수 조회
     * @return 미동기화 메시지 개수
     */
    suspend fun getUnsyncedCount(): CustomResult<Int, Exception>
    
    /**
     * 동기화 상태별 메시지 통계 조회
     * @return 상태별 메시지 개수 통계
     */
    suspend fun getSyncStatusStatistics(): CustomResult<Map<SyncStatus, Int>, Exception>

}