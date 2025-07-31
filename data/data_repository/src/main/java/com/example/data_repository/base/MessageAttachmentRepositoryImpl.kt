package com.example.data_repository.base

// import java.io.File // 안드로이드 Uri 대신 File 객체를 사용한다면
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data_datasource.remote.special.FileUploadDataSource
import com.example.data_datasource.remote.special.FileUploadResult
import com.example.data_model.remote.MessageAttachmentDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.example.domain_repository.base.FileUploadProgressData
import com.example.domain_repository.base.FileUploadResultData
import com.example.domain_repository.base.MessageAttachmentRepository
import com.example.mapper.DtoMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class MessageAttachmentRepositoryImpl @Inject constructor(
    messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSource,
    private val fileUploadDataSource: FileUploadDataSource,
    private val messageAttachmentMapper: DtoMapper<MessageAttachment, MessageAttachmentDTO>
    // private val localMediaDataSource: LocalMediaDataSource, // 파일 업로드 전처리 등에 사용 가능
) : DefaultRepositoryImpl<MessageAttachment, MessageAttachmentDTO>(
    messageAttachmentRemoteDataSource,
    messageAttachmentMapper
), MessageAttachmentRepository {

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
                    // MessageAttachment 엔티티 생성
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

    /**
     * Firebase Storage 경로를 생성합니다.
     * 예: "messages/{messageId}/attachments/{fileName}"
     */
    private fun generateStoragePath(messageId: DocumentId, attachmentType: MessageAttachmentType, fileName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return "messages/${messageId.value}/attachments/${attachmentType.value}_${timestamp}_${sanitizedFileName}"
    }
}
