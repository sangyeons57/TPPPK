package com.example.domain.usecase.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.repository.base.MessageAttachmentRepository
import com.example.domain.repository.base.FileUploadResultData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 메시지 첨부파일 업로드 UseCase
 */
interface UploadMessageAttachmentUseCase {
    operator fun invoke(
        fileUri: Uri,
        messageId: DocumentId
    ): Flow<FileUploadResultData>
}

class UploadMessageAttachmentUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val validateFileUseCase: ValidateFileUseCase,
    private val messageAttachmentRepository: MessageAttachmentRepository
) : UploadMessageAttachmentUseCase {

    override operator fun invoke(
        fileUri: Uri,
        messageId: DocumentId
    ): Flow<FileUploadResultData> = flow {
        try {
            // 1. 파일 검증
            val validationResult = validateFileUseCase(fileUri)
            when (validationResult) {
                is CustomResult.Success -> {
                    val fileInfo = validationResult.data
                    
                    // 2. 파일 메타데이터 추출
                    val fileName = extractFileName(fileUri)
                    val fileSize = fileInfo.fileSize

                    // 3. 파일 업로드 시작
                    messageAttachmentRepository.uploadFile(
                        fileUri = fileUri,
                        attachmentType = fileInfo.attachmentType,
                        fileName = fileName?.let { MessageAttachmentFileName(it) },
                        fileSize = MessageAttachmentFileSize(fileSize),
                        messageId = messageId
                    ).collect { result ->
                        emit(result)
                    }
                }
                is CustomResult.Failure -> {
                    emit(FileUploadResultData.Failure(validationResult.error))
                }
                is CustomResult.Initial -> {
                    // 초기 상태는 아직 처리하지 않음
                }
                is CustomResult.Loading -> {
                    // 로딩 상태는 진행률로 처리
                    emit(FileUploadResultData.Progress(com.example.domain.repository.base.FileUploadProgressData(0.0f, 0L, 0L)))
                }
                is CustomResult.Progress -> {
                    // 진행률 업데이트
                    emit(FileUploadResultData.Progress(com.example.domain.repository.base.FileUploadProgressData(validationResult.progress.toFloat(), 0L, 0L)))
                }
            }
        } catch (e: Exception) {
            emit(FileUploadResultData.Failure(e))
        }
    }

    /**
     * URI에서 파일명을 추출합니다.
     */
    private fun extractFileName(uri: Uri): String? {
        return try {
            var fileName: String? = null
            
            // ContentResolver를 사용하여 파일명 추출
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
            
            // ContentResolver로 파일명을 얻을 수 없는 경우, URI에서 추출
            fileName ?: uri.lastPathSegment
        } catch (e: Exception) {
            // 파일명을 추출할 수 없는 경우 기본값 사용
            "attachment_${System.currentTimeMillis()}"
        }
    }
}