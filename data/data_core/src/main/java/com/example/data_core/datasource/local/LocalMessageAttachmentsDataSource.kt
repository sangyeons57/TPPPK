package com.example.data_core.datasource.local

import com.example.domain.model.base.MessageAttachment
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 메시지 첨부파일 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalMessageAttachmentsDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 첨부파일 ID로 단일 첨부파일 조회
     * @param attachmentId 첨부파일 ID
     * @return 첨부파일 정보 (없으면 null)
     */
    suspend fun getAttachmentById(attachmentId: String): MessageAttachment?

    /**
     * 메시지별 첨부파일 목록 조회
     * @param messageId 메시지 ID
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByMessage(messageId: String): List<MessageAttachment>

    /**
     * 파일 타입별 첨부파일 조회
     * @param fileType 파일 타입 (IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER)
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByFileType(fileType: String): List<MessageAttachment>

    /**
     * 업로드 상태별 첨부파일 조회
     * @param uploadStatus 업로드 상태 (PENDING, UPLOADING, COMPLETED, FAILED)
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByUploadStatus(uploadStatus: String): List<MessageAttachment>

    /**
     * 채널별 첨부파일 목록 조회
     * @param channelId 채널 ID
     * @param fileType 특정 파일 타입 (선택적)
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByChannel(
        channelId: String,
        fileType: String? = null
    ): List<MessageAttachment>

    /**
     * 사용자별 첨부파일 목록 조회
     * @param userId 업로드한 사용자 ID
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByUser(userId: String): List<MessageAttachment>

    /**
     * 파일명으로 첨부파일 검색
     * @param query 검색 쿼리
     * @return 검색된 첨부파일 목록
     */
    suspend fun searchAttachmentsByFileName(query: String): List<MessageAttachment>

    /**
     * 단일 첨부파일 저장
     * @param attachment 저장할 첨부파일
     */
    suspend fun saveAttachment(attachment: MessageAttachment)

    /**
     * 첨부파일 목록 배치 저장
     * @param attachments 저장할 첨부파일 목록
     */
    suspend fun saveAttachments(attachments: List<MessageAttachment>)

    /**
     * 첨부파일 삭제
     * @param attachmentId 삭제할 첨부파일 ID
     */
    suspend fun deleteAttachment(attachmentId: String)

    /**
     * 메시지별 첨부파일 일괄 삭제
     * @param messageId 메시지 ID
     */
    suspend fun deleteAttachmentsByMessage(messageId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 첨부파일들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 첨부파일 목록
     */
    suspend fun getAttachmentsUpdatedAfter(timestamp: Instant): List<MessageAttachment>

    /**
     * 메시지별 첨부파일 실시간 관찰
     * @param messageId 메시지 ID
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByMessage(messageId: String): Flow<List<MessageAttachment>>

    /**
     * 특정 첨부파일 실시간 관찰
     * @param attachmentId 첨부파일 ID
     * @return 첨부파일 정보 Flow
     */
    fun observeAttachmentById(attachmentId: String): Flow<MessageAttachment?>

    /**
     * 업로드 상태별 첨부파일 실시간 관찰
     * @param uploadStatus 업로드 상태
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByUploadStatus(uploadStatus: String): Flow<List<MessageAttachment>>

    // === Outbox 관리 ===

    /**
     * 첨부파일 변경사항을 Outbox에 기록
     * @param attachmentId 첨부파일 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(attachmentId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<MessageAttachmentOutboxOperation>

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
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    // === 유틸리티 ===

    /**
     * 첨부파일 존재 여부 확인
     * @param attachmentId 첨부파일 ID
     * @return 존재 여부
     */
    suspend fun attachmentExists(attachmentId: String): Boolean

    /**
     * 메시지별 첨부파일 수 조회
     * @param messageId 메시지 ID
     * @return 첨부파일 수
     */
    suspend fun getAttachmentCount(messageId: String): Int

    /**
     * 전체 첨부파일 수 조회
     * @return 전체 첨부파일 수
     */
    suspend fun getTotalAttachmentCount(): Int

    /**
     * 파일 타입별 첨부파일 수 조회
     * @param fileType 파일 타입
     * @return 첨부파일 수
     */
    suspend fun getAttachmentCountByFileType(fileType: String): Int

    /**
     * 업로드 실패한 첨부파일 목록 조회
     * @return 실패한 첨부파일 목록
     */
    suspend fun getFailedAttachments(): List<MessageAttachment>

    /**
     * 모든 첨부파일 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllAttachments()
}

/**
 * 메시지 첨부파일 Outbox 작업 정보
 */
data class MessageAttachmentOutboxOperation(
    val id: String,
    val attachmentId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)