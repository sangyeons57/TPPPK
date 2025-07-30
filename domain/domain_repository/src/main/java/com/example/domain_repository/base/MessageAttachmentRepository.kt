package com.example.domain_repository.base

import android.net.Uri
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain_repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * 파일 업로드 진행률 데이터
 */
data class FileUploadProgressData(
    val progress: Float, // 0.0 ~ 1.0
    val bytesTransferred: Long,
    val totalBytes: Long
)

/**
 * 파일 업로드 결과
 */
sealed class FileUploadResultData {
    data class Progress(val progressData: FileUploadProgressData) : FileUploadResultData()
    data class Success(val attachment: MessageAttachment) : FileUploadResultData()
    data class Failure(val exception: Exception) : FileUploadResultData()
}

interface MessageAttachmentRepository : DefaultRepository {
    
    /**
     * 파일을 업로드하고 MessageAttachment를 생성합니다.
     * @param fileUri 업로드할 파일의 URI
     * @param attachmentType 첨부파일 타입
     * @param fileName 파일명 (선택적)
     * @param fileSize 파일 크기 (선택적)
     * @param messageId 메시지 ID (Storage 경로 생성용)
     * @return 업로드 진행률과 결과를 스트리밍하는 Flow
     */
    fun uploadFile(
        fileUri: Uri,
        attachmentType: MessageAttachmentType,
        fileName: MessageAttachmentFileName?,
        fileSize: MessageAttachmentFileSize?,
        messageId: DocumentId
    ): Flow<FileUploadResultData>
}
