package com.example.data.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.datasource.remote.special.FileUploadDataSource
import com.example.data.datasource.remote.special.FileUploadResult
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.example.domain.repository.base.MessageAttachmentRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.base.FileUploadProgressData
import com.example.domain.repository.base.FileUploadResultData
import com.example.domain.repository.factory.context.MessageAttachmentRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Remote MessageAttachment Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class MessageAttachmentRepositoryImpl @Inject constructor(
    private val messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource,
    private val fileUploadDataSource: FileUploadDataSource,
    override val factoryContext: MessageAttachmentRepositoryFactoryContext
) : MessageAttachmentRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        messageId: String?
    ): CustomResult<SyncResult<MessageAttachment>, Exception> {
        return messageAttachmentRemoteDataSource.syncFromServer(lastSyncCursor, messageId)
    }

    override suspend fun syncToServer(
        messageId: String?
    ): CustomResult<Int, Exception> {
        return messageAttachmentRemoteDataSource.syncToServer(messageId)
    }

    override suspend fun forceSyncAll(
        messageId: String?
    ): CustomResult<Int, Exception> {
        return messageAttachmentRemoteDataSource.forceSyncAll(messageId)
    }

    override suspend fun resolveConflicts(
        conflictedAttachmentIds: List<String>
    ): CustomResult<Int, Exception> {
        return messageAttachmentRemoteDataSource.resolveConflicts(conflictedAttachmentIds)
    }

    // === Firebase Storage 작업 ===

    override fun uploadFile(
        fileUri: Uri,
        attachmentType: MessageAttachmentType,
        fileName: MessageAttachmentFileName?,
        fileSize: MessageAttachmentFileSize?,
        messageId: DocumentId
    ): Flow<FileUploadResultData> {
        return fileUploadDataSource.uploadFileWithProgress(
            storagePath = generateStoragePath(messageId, attachmentType, fileName?.value ?: "file"),
            fileUri = fileUri
        ).map { result ->
            when (result) {
                is FileUploadResult.Progress -> {
                    FileUploadResultData.Progress(
                        FileUploadProgressData(
                            progress = result.progress.progress,
                            bytesTransferred = result.progress.bytesTransferred,
                            totalBytes = result.progress.totalBytes
                        )
                    )
                }
                is FileUploadResult.Success -> {
                    val attachmentId = DocumentId(UUID.randomUUID().toString())
                    val attachment = MessageAttachment.create(
                        id = attachmentId,
                        attachmentType = attachmentType,
                        attachmentUrl = MessageAttachmentUrl.fromString(result.downloadUrl),
                        fileName = fileName,
                        fileSize = fileSize,
                        uploadStatus = MessageAttachmentUploadStatus.COMPLETED,
                        uploadProgress = MessageAttachmentUploadProgress.complete()
                    )
                    
                    FileUploadResultData.Success(attachment)
                }
                is FileUploadResult.Failure -> {
                    FileUploadResultData.Failure(result.exception)
                }
            }
        }
    }

    private fun generateStoragePath(messageId: DocumentId, attachmentType: MessageAttachmentType, fileName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return "messages/${messageId.value}/attachments/${attachmentType.value}_${timestamp}_${sanitizedFileName}"
    }
}
