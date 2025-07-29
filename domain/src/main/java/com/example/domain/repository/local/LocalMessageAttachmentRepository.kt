package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Message Attachment Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote MessageAttachmentRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - MessageAttachment 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 * - 업로드 상태 관리 및 추적
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeAttachmentById
 * - observeAllEntities -> observeAllAttachments
 * - observeEntityUpdatedAt -> observeAttachmentUpdatedAt
 * - getEntityById -> getAttachmentById
 * - getEntitiesByIds -> getAttachmentsByIds
 * - getAllEntities -> getAllAttachments
 * - saveEntity -> saveAttachment
 * - saveEntities -> saveAttachments
 * - deleteEntity -> deleteAttachment
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalMessageAttachmentRepository : BaseLocalRepository<MessageAttachment> {

    // === BaseLocalRepository 메서드 (구현체에서 첨부파일 전용 메서드로 매핑) ===
    // observeEntityById -> observeAttachmentById
    // observeAllEntities -> observeAllAttachments  
    // observeEntityUpdatedAt -> observeAttachmentUpdatedAt
    // getEntityById -> getAttachmentById
    // getEntitiesByIds -> getAttachmentsByIds
    // getAllEntities -> getAllAttachments
    // saveEntity -> saveAttachment
    // saveEntities -> saveAttachments
    // deleteEntity -> deleteAttachment
    // getEntitiesUpdatedAfter -> getAttachmentsUpdatedAfter
    // clearAllEntities -> clearAllAttachments
    // getTotalEntityCount -> getTotalAttachmentCount
    // entityExists -> attachmentExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 메시지 첨부파일을 실시간 관찰
     * @param attachmentId 첨부파일 ID
     * @return 첨부파일 Flow (null 가능)
     */
    fun observeAttachmentById(attachmentId: String): Flow<MessageAttachment?>

    /**
     * 메시지별 첨부파일 목록을 실시간 관찰
     * @param messageId 메시지 ID
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByMessage(messageId: String): Flow<List<MessageAttachment>>

    /**
     * 특정 파일 타입의 첨부파일들을 실시간 관찰
     * @param attachmentType 첨부파일 타입
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByType(attachmentType: MessageAttachmentType): Flow<List<MessageAttachment>>

    /**
     * 특정 업로드 상태의 첨부파일들을 실시간 관찰
     * @param uploadStatus 업로드 상태
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByUploadStatus(uploadStatus: MessageAttachmentUploadStatus): Flow<List<MessageAttachment>>

    /**
     * 주어진 ID 목록에 해당하는 첨부파일 목록을 실시간 관찰
     * @param attachmentIds 첨부파일 ID 목록
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachments(attachmentIds: List<String>): Flow<List<MessageAttachment>>

    /**
     * 특정 첨부파일의 updatedAt 필드 변경을 실시간 관찰
     * @param attachmentId 첨부파일 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeAttachmentUpdatedAt(attachmentId: String): Flow<Long?>

    /**
     * 모든 첨부파일을 실시간 관찰
     * @return 전체 첨부파일 목록 Flow
     */
    fun observeAllAttachments(): Flow<List<MessageAttachment>>

    /**
     * 채널별 첨부파일 목록을 실시간 관찰
     * @param channelId 채널 ID
     * @param attachmentType 특정 파일 타입 (선택적)
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByChannel(
        channelId: String,
        attachmentType: MessageAttachmentType? = null
    ): Flow<List<MessageAttachment>>

    /**
     * 사용자별 첨부파일 목록을 실시간 관찰
     * @param userId 업로드한 사용자 ID
     * @return 첨부파일 목록 Flow
     */
    fun observeAttachmentsByUser(userId: String): Flow<List<MessageAttachment>>

    /**
     * 업로드 진행 상황을 실시간 관찰 (진행중인 업로드만)
     * @return 업로드 중인 첨부파일 목록 Flow
     */
    fun observeUploadingAttachments(): Flow<List<MessageAttachment>>

    // === 단순 읽기 작업 ===

    /**
     * 첨부파일 ID로 조회
     * @param attachmentId 첨부파일 ID
     * @return 첨부파일 (없으면 null)
     */
    suspend fun getAttachmentById(attachmentId: String): MessageAttachment?

    /**
     * 메시지별 첨부파일 목록 조회
     * @param messageId 메시지 ID
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByMessage(messageId: String): List<MessageAttachment>

    /**
     * 파일명으로 첨부파일 검색 (부분 일치)
     * @param fileName 검색할 파일명
     * @param limit 제한 개수
     * @return 첨부파일 목록
     */
    suspend fun searchAttachmentsByFileName(
        fileName: String,
        limit: Int = 10
    ): List<MessageAttachment>

    /**
     * 여러 첨부파일 ID로 조회
     * @param attachmentIds 첨부파일 ID 목록
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByIds(attachmentIds: List<String>): List<MessageAttachment>

    /**
     * 전체 첨부파일 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 첨부파일 목록
     */
    suspend fun getAllAttachments(limit: Int? = null): List<MessageAttachment>

    /**
     * 특정 파일 타입의 첨부파일들 조회
     * @param attachmentType 첨부파일 타입
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByType(attachmentType: MessageAttachmentType): List<MessageAttachment>

    /**
     * 특정 업로드 상태의 첨부파일들 조회
     * @param uploadStatus 업로드 상태
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByUploadStatus(uploadStatus: MessageAttachmentUploadStatus): List<MessageAttachment>

    /**
     * 채널별 첨부파일 목록 조회
     * @param channelId 채널 ID
     * @param attachmentType 특정 파일 타입 (선택적)
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByChannel(
        channelId: String,
        attachmentType: MessageAttachmentType? = null
    ): List<MessageAttachment>

    /**
     * 사용자별 첨부파일 목록 조회
     * @param userId 업로드한 사용자 ID
     * @return 첨부파일 목록
     */
    suspend fun getAttachmentsByUser(userId: String): List<MessageAttachment>

    /**
     * 업로드 실패한 첨부파일 목록 조회
     * @return 실패한 첨부파일 목록
     */
    suspend fun getFailedAttachments(): List<MessageAttachment>

    /**
     * 업로드 진행중인 첨부파일 목록 조회
     * @return 진행중인 첨부파일 목록
     */
    suspend fun getUploadingAttachments(): List<MessageAttachment>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 첨부파일 저장 (생성/수정)
     * @param attachment 저장할 첨부파일
     * @return 성공 여부
     */
    suspend fun saveAttachment(attachment: MessageAttachment): CustomResult<Unit, Exception>

    /**
     * 첨부파일 대량 저장 (동기화용)
     * @param attachments 저장할 첨부파일 목록
     * @return 성공 여부
     */
    suspend fun saveAttachments(attachments: List<MessageAttachment>): CustomResult<Unit, Exception>

    /**
     * 첨부파일 삭제
     * @param attachmentId 첨부파일 ID
     * @return 성공 여부
     */
    suspend fun deleteAttachment(attachmentId: String): CustomResult<Unit, Exception>

    /**
     * 메시지별 첨부파일 일괄 삭제
     * @param messageId 메시지 ID
     * @return 성공 여부
     */
    suspend fun deleteAttachmentsByMessage(messageId: String): CustomResult<Unit, Exception>

    /**
     * 첨부파일 업로드 상태 업데이트 (로컬)
     * @param attachmentId 첨부파일 ID
     * @param uploadStatus 새로운 업로드 상태
     * @param uploadProgress 업로드 진행률 (nullable)
     * @param errorMessage 에러 메시지 (실패시)
     * @return 성공 여부
     */
    suspend fun updateUploadStatus(
        attachmentId: String,
        uploadStatus: MessageAttachmentUploadStatus,
        uploadProgress: MessageAttachmentUploadProgress? = null,
        errorMessage: String? = null
    ): CustomResult<Unit, Exception>

    /**
     * 첨부파일 업로드 진행률 업데이트
     * @param attachmentId 첨부파일 ID
     * @param progress 업로드 진행률
     * @return 성공 여부
     */
    suspend fun updateUploadProgress(
        attachmentId: String,
        progress: MessageAttachmentUploadProgress
    ): CustomResult<Unit, Exception>

    /**
     * 첨부파일 메타데이터 업데이트 (로컬)
     * @param attachmentId 첨부파일 ID
     * @param fileName 새로운 파일명 (nullable)
     * @param fileSize 새로운 파일 크기 (nullable)
     * @param thumbnailUrl 새로운 썸네일 URL (nullable)
     * @return 성공 여부
     */
    suspend fun updateAttachmentMetadata(
        attachmentId: String,
        fileName: MessageAttachmentFileName? = null,
        fileSize: MessageAttachmentFileSize? = null,
        thumbnailUrl: String? = null
    ): CustomResult<Unit, Exception>

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
     * @return 첨부파일 수
     */
    suspend fun getTotalAttachmentCount(): Int

    /**
     * 특정 파일 타입의 첨부파일 수 조회
     * @param attachmentType 첨부파일 타입
     * @return 첨부파일 수
     */
    suspend fun getAttachmentCountByType(attachmentType: MessageAttachmentType): Int

    /**
     * 특정 업로드 상태의 첨부파일 수 조회
     * @param uploadStatus 업로드 상태
     * @return 첨부파일 수
     */
    suspend fun getAttachmentCountByStatus(uploadStatus: MessageAttachmentUploadStatus): Int

    /**
     * 채널별 첨부파일 수 조회
     * @param channelId 채널 ID
     * @return 첨부파일 수
     */
    suspend fun getAttachmentCountByChannel(channelId: String): Int

    /**
     * 사용자별 첨부파일 수 조회
     * @param userId 사용자 ID
     * @return 첨부파일 수
     */
    suspend fun getAttachmentCountByUser(userId: String): Int

    /**
     * 모든 첨부파일 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllAttachments(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 첨부파일 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 첨부파일 목록
     */
    suspend fun getAttachmentsUpdatedAfter(timestamp: Instant): List<MessageAttachment>

}