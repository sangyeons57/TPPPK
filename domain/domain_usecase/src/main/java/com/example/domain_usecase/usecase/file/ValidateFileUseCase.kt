package com.example.domain_usecase.usecase.file

import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.util.StreamingFileValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 파일 검증 결과
 */
data class FileValidationResult(
    val isValid: Boolean,
    val fileSize: Long,
    val mimeType: String?,
    val attachmentType: MessageAttachmentType,
    val errorMessage: String? = null
)

/**
 * 파일 검증 에러
 */
sealed class FileValidationError : Exception() {
    data class FileTooLarge(val maxSize: Long, val actualSize: Long) : FileValidationError() {
        override val message: String = "파일 크기가 너무 큽니다. 최대 ${maxSize / (1024 * 1024)}MB까지 업로드 가능합니다."
    }
    
    data class UnsupportedFileType(val mimeType: String?) : FileValidationError() {
        override val message: String = "지원하지 않는 파일 형식입니다: $mimeType"
    }
    
    data class FileNotFound(val uri: Uri) : FileValidationError() {
        override val message: String = "파일을 찾을 수 없습니다: $uri"
    }
    
    data class FileAccessError(override val cause: Throwable) : FileValidationError() {
        override val message: String = "파일에 접근할 수 없습니다: ${cause.message}"
    }
}

/**
 * 파일 검증 UseCase
 */
interface ValidateFileUseCase {
    suspend operator fun invoke(fileUri: Uri): CustomResult<FileValidationResult, FileValidationError>
}

class ValidateFileUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamingValidator: StreamingFileValidator
) : ValidateFileUseCase {

    companion object {
        // 파일 크기 제한 (MB 단위)
        private const val MAX_IMAGE_SIZE_MB = 10L
        private const val MAX_VIDEO_SIZE_MB = 100L
        private const val MAX_FILE_SIZE_MB = 50L
        private const val MAX_AUDIO_SIZE_MB = 20L
        
        // 지원하는 MIME 타입
        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
        )
        
        private val SUPPORTED_VIDEO_TYPES = setOf(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm", "video/mkv"
        )
        
        private val SUPPORTED_AUDIO_TYPES = setOf(
            "audio/mp3", "audio/wav", "audio/aac", "audio/ogg", "audio/m4a", "audio/flac"
        )
        
        private val SUPPORTED_DOCUMENT_TYPES = setOf(
            "application/pdf", "text/plain", "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", 
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    }

    override suspend operator fun invoke(fileUri: Uri): CustomResult<FileValidationResult, FileValidationError> {
        return try {
            // 스트리밍 파일 검증 수행
            val streamingResult = streamingValidator.validateStreamingFile(fileUri)
            
            val validationData = when (streamingResult) {
                is CustomResult.Success -> streamingResult.data
                is CustomResult.Failure -> {
                    // StreamingValidationError를 FileValidationError로 변환
                    val fileValidationError = when (streamingResult.error) {
                        is com.example.domain.util.StreamingValidationError.FileNotAccessible -> 
                            FileValidationError.FileNotFound(fileUri)
                        is com.example.domain.util.StreamingValidationError.FileSizeDetectionFailed -> 
                            FileValidationError.FileAccessError(streamingResult.error.cause ?: Exception("Unknown cause"))
                        is com.example.domain.util.StreamingValidationError.MimeTypeDetectionFailed -> 
                            FileValidationError.UnsupportedFileType("unknown")
                        is com.example.domain.util.StreamingValidationError.MagicNumberValidationFailed -> {
                            val magicError = streamingResult.error as com.example.domain.util.StreamingValidationError.MagicNumberValidationFailed
                            FileValidationError.UnsupportedFileType(magicError.expectedType)
                        }
                    }
                    return CustomResult.Failure(fileValidationError)
                }
                is CustomResult.Initial -> {
                    // 초기 상태는 아직 처리하지 않음
                    return CustomResult.Failure(FileValidationError.FileAccessError(IllegalStateException("초기 상태에서 검증 수행 불가")))
                }
                is CustomResult.Loading -> {
                    // 로딩 상태는 계속 대기
                    return CustomResult.Failure(FileValidationError.FileAccessError(IllegalStateException("로딩 중입니다")))
                }
                is CustomResult.Progress -> {
                    // 진행 상태는 아직 완료되지 않음
                    return CustomResult.Failure(FileValidationError.FileAccessError(IllegalStateException("검증 진행 중입니다")))
                }
            }

            // 파일 크기 검증
            val maxSize = when (validationData.attachmentType) {
                MessageAttachmentType.IMAGE -> MAX_IMAGE_SIZE_MB * 1024 * 1024
                MessageAttachmentType.VIDEO -> MAX_VIDEO_SIZE_MB * 1024 * 1024
                MessageAttachmentType.AUDIO -> MAX_AUDIO_SIZE_MB * 1024 * 1024
                MessageAttachmentType.FILE -> MAX_FILE_SIZE_MB * 1024 * 1024
                MessageAttachmentType.UNKNOWN -> MAX_FILE_SIZE_MB * 1024 * 1024
                MessageAttachmentType.LINK -> Long.MAX_VALUE // 링크는 크기 제한 없음
            }

            if (validationData.fileSize > maxSize) {
                return CustomResult.Failure(FileValidationError.FileTooLarge(maxSize, validationData.fileSize))
            }

            // 파일 타입 지원 여부 검증
            if (!isFileTypeSupported(validationData.mimeType, validationData.attachmentType)) {
                return CustomResult.Failure(FileValidationError.UnsupportedFileType(validationData.mimeType))
            }

            // 최종 검증 결과 생성
            val result = FileValidationResult(
                isValid = true,
                fileSize = validationData.fileSize,
                mimeType = validationData.mimeType,
                attachmentType = validationData.attachmentType
            )

            CustomResult.Success(result)

        } catch (e: Exception) {
            CustomResult.Failure(FileValidationError.FileAccessError(e))
        }
    }


    private fun isFileTypeSupported(mimeType: String?, attachmentType: MessageAttachmentType): Boolean {
        if (mimeType == null) return false

        return when (attachmentType) {
            MessageAttachmentType.IMAGE -> mimeType in SUPPORTED_IMAGE_TYPES
            MessageAttachmentType.VIDEO -> mimeType in SUPPORTED_VIDEO_TYPES
            MessageAttachmentType.AUDIO -> mimeType in SUPPORTED_AUDIO_TYPES
            MessageAttachmentType.FILE -> mimeType in SUPPORTED_DOCUMENT_TYPES || mimeType.startsWith("text/")
            MessageAttachmentType.UNKNOWN -> false // UNKNOWN 타입은 지원하지 않음
            MessageAttachmentType.LINK -> true // 링크는 항상 지원
        }
    }
}