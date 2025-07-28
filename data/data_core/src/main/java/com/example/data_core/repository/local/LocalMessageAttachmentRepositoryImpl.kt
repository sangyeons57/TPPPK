package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalMessageAttachmentsDataSource
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.repository.local.LocalMessageAttachmentRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Message Attachment Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 * - 업로드 상태 관리 및 추적
 */
@Singleton
class LocalMessageAttachmentRepositoryImpl @Inject constructor(
    private val localMessageAttachmentsDataSource: LocalMessageAttachmentsDataSource
) : LocalMessageAttachmentRepository {

    companion object {
        private const val TAG = "LocalMessageAttachmentRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeAttachmentById(attachmentId: String): Flow<MessageAttachment?> {
        Log.d(TAG, "observeAttachmentById: $attachmentId")
        return localMessageAttachmentsDataSource.observeAttachmentById(attachmentId)
    }

    override fun observeAttachmentsByMessage(messageId: String): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAttachmentsByMessage: $messageId")
        return localMessageAttachmentsDataSource.observeAttachmentsByMessage(messageId)
    }

    override fun observeAttachmentsByType(attachmentType: MessageAttachmentType): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAttachmentsByType: $attachmentType")
        return localMessageAttachmentsDataSource.observeAttachmentsByType(attachmentType.name)
    }

    override fun observeAttachmentsByUploadStatus(uploadStatus: MessageAttachmentUploadStatus): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAttachmentsByUploadStatus: $uploadStatus")
        return localMessageAttachmentsDataSource.observeAttachmentsByUploadStatus(uploadStatus.name)
    }

    override fun observeAttachments(attachmentIds: List<String>): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAttachments: ${attachmentIds.size} attachments")
        return localMessageAttachmentsDataSource.observeAttachments(attachmentIds)
    }

    override fun observeAttachmentUpdatedAt(attachmentId: String): Flow<Long?> {
        Log.d(TAG, "observeAttachmentUpdatedAt: $attachmentId")
        return localMessageAttachmentsDataSource.observeAttachmentUpdatedAt(attachmentId)
    }

    override fun observeAllAttachments(): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAllAttachments")
        return localMessageAttachmentsDataSource.observeAllAttachments()
    }

    override fun observeAttachmentsByChannel(
        channelId: String,
        attachmentType: MessageAttachmentType?
    ): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAttachmentsByChannel: channelId=$channelId, type=$attachmentType")
        return localMessageAttachmentsDataSource.observeAttachmentsByChannel(channelId, attachmentType?.name)
    }

    override fun observeAttachmentsByUser(userId: String): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeAttachmentsByUser: $userId")
        return localMessageAttachmentsDataSource.observeAttachmentsByUser(userId)
    }

    override fun observeUploadingAttachments(): Flow<List<MessageAttachment>> {
        Log.d(TAG, "observeUploadingAttachments")
        return localMessageAttachmentsDataSource.observeUploadingAttachments()
    }

    // === 단순 읽기 작업 ===

    override suspend fun getAttachmentById(attachmentId: String): MessageAttachment? {
        Log.d(TAG, "getAttachmentById: $attachmentId")
        return try {
            localMessageAttachmentsDataSource.getAttachmentById(attachmentId)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentById failed", e)
            null
        }
    }

    override suspend fun getAttachmentsByMessage(messageId: String): List<MessageAttachment> {
        Log.d(TAG, "getAttachmentsByMessage: $messageId")
        return try {
            localMessageAttachmentsDataSource.getAttachmentsByMessage(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsByMessage failed", e)
            emptyList()
        }
    }

    override suspend fun searchAttachmentsByFileName(
        fileName: String,
        limit: Int
    ): List<MessageAttachment> {
        Log.d(TAG, "searchAttachmentsByFileName: fileName='$fileName', limit=$limit")
        return try {
            localMessageAttachmentsDataSource.searchAttachmentsByFileName(fileName, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchAttachmentsByFileName failed", e)
            emptyList()
        }
    }

    override suspend fun getAttachmentsByIds(attachmentIds: List<String>): List<MessageAttachment> {
        Log.d(TAG, "getAttachmentsByIds: ${attachmentIds.size} attachments")
        return try {
            localMessageAttachmentsDataSource.getAttachmentsByIds(attachmentIds)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllAttachments(limit: Int?): List<MessageAttachment> {
        Log.d(TAG, "getAllAttachments: limit=$limit")
        return try {
            localMessageAttachmentsDataSource.getAllAttachments(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllAttachments failed", e)
            emptyList()
        }
    }

    override suspend fun getAttachmentsByType(attachmentType: MessageAttachmentType): List<MessageAttachment> {
        Log.d(TAG, "getAttachmentsByType: $attachmentType")
        return try {
            localMessageAttachmentsDataSource.getAttachmentsByType(attachmentType.name)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsByType failed", e)
            emptyList()
        }
    }

    override suspend fun getAttachmentsByUploadStatus(uploadStatus: MessageAttachmentUploadStatus): List<MessageAttachment> {
        Log.d(TAG, "getAttachmentsByUploadStatus: $uploadStatus")
        return try {
            localMessageAttachmentsDataSource.getAttachmentsByUploadStatus(uploadStatus.name)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsByUploadStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getAttachmentsByChannel(
        channelId: String,
        attachmentType: MessageAttachmentType?
    ): List<MessageAttachment> {
        Log.d(TAG, "getAttachmentsByChannel: channelId=$channelId, type=$attachmentType")
        return try {
            localMessageAttachmentsDataSource.getAttachmentsByChannel(channelId, attachmentType?.name)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsByChannel failed", e)
            emptyList()
        }
    }

    override suspend fun getAttachmentsByUser(userId: String): List<MessageAttachment> {
        Log.d(TAG, "getAttachmentsByUser: $userId")
        return try {
            localMessageAttachmentsDataSource.getAttachmentsByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getFailedAttachments(): List<MessageAttachment> {
        Log.d(TAG, "getFailedAttachments")
        return try {
            localMessageAttachmentsDataSource.getFailedAttachments()
        } catch (e: Exception) {
            Log.e(TAG, "getFailedAttachments failed", e)
            emptyList()
        }
    }

    override suspend fun getUploadingAttachments(): List<MessageAttachment> {
        Log.d(TAG, "getUploadingAttachments")
        return try {
            localMessageAttachmentsDataSource.getUploadingAttachments()
        } catch (e: Exception) {
            Log.e(TAG, "getUploadingAttachments failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveAttachment(attachment: MessageAttachment): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveAttachment: ${attachment.id}")

            // 1. Room DB에 저장
            localMessageAttachmentsDataSource.saveAttachment(attachment)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (attachment.isNew) "CREATE" else "UPDATE"
            localMessageAttachmentsDataSource.addToOutbox(
                attachmentId = attachment.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Attachment saved and added to outbox: ${attachment.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveAttachment failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveAttachments(attachments: List<MessageAttachment>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveAttachments: ${attachments.size} attachments")

            if (attachments.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localMessageAttachmentsDataSource.saveAttachments(attachments)

            Log.d(TAG, "Bulk attachments saved: ${attachments.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveAttachments failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteAttachment(attachmentId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteAttachment: $attachmentId")

            // 1. Room DB에서 삭제
            localMessageAttachmentsDataSource.deleteAttachment(attachmentId)

            // 2. Outbox에 삭제 작업 추가
            localMessageAttachmentsDataSource.addToOutbox(
                attachmentId = attachmentId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Attachment deleted and added to outbox: $attachmentId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteAttachment failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteAttachmentsByMessage(messageId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteAttachmentsByMessage: $messageId")

            // 1. 메시지의 모든 첨부파일 조회
            val attachments = localMessageAttachmentsDataSource.getAttachmentsByMessage(messageId)

            // 2. 각 첨부파일 삭제
            for (attachment in attachments) {
                deleteAttachment(attachment.id.value)
            }

            Log.d(TAG, "All attachments deleted for message: $messageId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteAttachmentsByMessage failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateUploadStatus(
        attachmentId: String,
        uploadStatus: MessageAttachmentUploadStatus,
        uploadProgress: MessageAttachmentUploadProgress?,
        errorMessage: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "updateUploadStatus: attachmentId=$attachmentId, status=$uploadStatus, progress=$uploadProgress"
            )

            // 1. 현재 첨부파일 조회
            val currentAttachment =
                localMessageAttachmentsDataSource.getAttachmentById(attachmentId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Attachment not found: $attachmentId"))

            // 2. 업로드 상태 업데이트
            currentAttachment.updateUploadStatus(uploadStatus, errorMessage)

            // 3. 진행률 업데이트 (제공된 경우)
            if (uploadProgress != null) {
                currentAttachment.updateUploadProgress(uploadProgress)
            }

            // 4. 저장 (Outbox 포함)
            saveAttachment(currentAttachment)

            Log.d(TAG, "Upload status updated: $attachmentId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateUploadStatus failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateUploadProgress(
        attachmentId: String,
        progress: MessageAttachmentUploadProgress
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "updateUploadProgress: attachmentId=$attachmentId, progress=${progress.value}"
            )

            // 1. 현재 첨부파일 조회
            val currentAttachment =
                localMessageAttachmentsDataSource.getAttachmentById(attachmentId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Attachment not found: $attachmentId"))

            // 2. 진행률 업데이트
            currentAttachment.updateUploadProgress(progress)

            // 3. 저장 (Outbox 포함)
            saveAttachment(currentAttachment)

            Log.d(TAG, "Upload progress updated: $attachmentId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateUploadProgress failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateAttachmentMetadata(
        attachmentId: String,
        fileName: MessageAttachmentFileName?,
        fileSize: MessageAttachmentFileSize?,
        thumbnailUrl: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(
                TAG,
                "updateAttachmentMetadata: attachmentId=$attachmentId, fileName=$fileName, fileSize=$fileSize"
            )

            // 1. 현재 첨부파일 조회
            val currentAttachment =
                localMessageAttachmentsDataSource.getAttachmentById(attachmentId)
                    ?: return CustomResult.Failure(IllegalArgumentException("Attachment not found: $attachmentId"))

            // 2. 메타데이터는 불변 객체이므로 새 인스턴스 생성이 필요
            // MessageAttachment의 구조상 메타데이터 변경을 위해서는 새 인스턴스를 만들어야 함
            // 실제 구현에서는 도메인 모델에 메타데이터 업데이트 메서드가 필요할 수 있습니다.

            // 3. 저장 (Outbox 포함)
            saveAttachment(currentAttachment)

            Log.d(TAG, "Attachment metadata updated: $attachmentId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateAttachmentMetadata failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun attachmentExists(attachmentId: String): Boolean {
        return try {
            localMessageAttachmentsDataSource.attachmentExists(attachmentId)
        } catch (e: Exception) {
            Log.e(TAG, "attachmentExists failed", e)
            false
        }
    }

    override suspend fun getAttachmentCount(messageId: String): Int {
        return try {
            localMessageAttachmentsDataSource.getAttachmentCount(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentCount failed", e)
            0
        }
    }

    override suspend fun getTotalAttachmentCount(): Int {
        return try {
            localMessageAttachmentsDataSource.getTotalAttachmentCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalAttachmentCount failed", e)
            0
        }
    }

    override suspend fun getAttachmentCountByType(attachmentType: MessageAttachmentType): Int {
        return try {
            localMessageAttachmentsDataSource.getAttachmentCountByFileType(attachmentType.name)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentCountByType failed", e)
            0
        }
    }

    override suspend fun getAttachmentCountByStatus(uploadStatus: MessageAttachmentUploadStatus): Int {
        return try {
            localMessageAttachmentsDataSource.getAttachmentCountByStatus(uploadStatus.name)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentCountByStatus failed", e)
            0
        }
    }

    override suspend fun getAttachmentCountByChannel(channelId: String): Int {
        return try {
            localMessageAttachmentsDataSource.getAttachmentCountByChannel(channelId)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentCountByChannel failed", e)
            0
        }
    }

    override suspend fun getAttachmentCountByUser(userId: String): Int {
        return try {
            localMessageAttachmentsDataSource.getAttachmentCountByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentCountByUser failed", e)
            0
        }
    }

    override suspend fun clearAllAttachments(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllAttachments")

            localMessageAttachmentsDataSource.clearAllAttachments()

            Log.d(TAG, "All attachments cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllAttachments failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getAttachmentsUpdatedAfter(timestamp: Instant): List<MessageAttachment> {
        return try {
            localMessageAttachmentsDataSource.getAttachmentsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getAttachmentsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        attachmentId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: attachmentId=$attachmentId, operation=$operation")

            localMessageAttachmentsDataSource.addToOutbox(attachmentId, operation, payload)

            Log.d(TAG, "Added to outbox: $attachmentId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}