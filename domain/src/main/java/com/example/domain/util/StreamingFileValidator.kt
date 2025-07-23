package com.example.domain.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.MessageAttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// import timber.log.Timber - Domain 모듈에서 제거됨
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * 스트리밍 파일 검증 에러
 */
sealed class StreamingValidationError : Exception() {
    data class FileNotAccessible(val uri: Uri, override val cause: Throwable? = null) : StreamingValidationError() {
        override val message: String = "파일에 접근할 수 없습니다: $uri"
    }
    
    data class FileSizeDetectionFailed(val uri: Uri, override val cause: Throwable) : StreamingValidationError() {
        override val message: String = "파일 크기를 확인할 수 없습니다: ${cause.message}"
    }
    
    data class MimeTypeDetectionFailed(val uri: Uri) : StreamingValidationError() {
        override val message: String = "파일 형식을 확인할 수 없습니다"
    }
    
    data class MagicNumberValidationFailed(val uri: Uri, val expectedType: String, val actualSignature: String) : StreamingValidationError() {
        override val message: String = "파일 내용이 예상된 형식($expectedType)과 일치하지 않습니다"
    }
}

/**
 * 스트리밍 파일 검증 결과
 */
data class StreamingValidationResult(
    val fileSize: Long,
    val mimeType: String,
    val attachmentType: MessageAttachmentType,
    val fileName: String?,
    val isContentValid: Boolean = true,
    val magicNumberMatch: Boolean = true
)

/**
 * 메모리 효율적인 스트리밍 방식으로 파일을 검증하는 유틸리티
 */
class StreamingFileValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // 매직 넘버 검증을 위한 시그니처
        private val FILE_SIGNATURES = mapOf(
            "image/jpeg" to listOf(
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()),
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte())
            ),
            "image/png" to listOf(
                byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
            ),
            "image/gif" to listOf(
                byteArrayOf(0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x37.toByte(), 0x61.toByte()),
                byteArrayOf(0x47.toByte(), 0x49.toByte(), 0x46.toByte(), 0x38.toByte(), 0x39.toByte(), 0x61.toByte())
            ),
            "application/pdf" to listOf(
                byteArrayOf(0x25.toByte(), 0x50.toByte(), 0x44.toByte(), 0x46.toByte())
            ),
            "video/mp4" to listOf(
                byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x18.toByte(), 0x66.toByte(), 0x74.toByte(), 0x79.toByte(), 0x70.toByte())
            )
        )
    }

    /**
     * 스트리밍 방식으로 파일 크기를 정확하게 계산합니다.
     */
    suspend fun getAccurateFileSize(uri: Uri): CustomResult<Long, StreamingValidationError> = withContext(Dispatchers.IO) {
        try {
            // 1차: 메타데이터 쿼리 (가장 정확하고 빠름)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) {
                            println("DEBUG: File size from metadata: $size bytes")
                            return@withContext CustomResult.Success(size)
                        }
                    }
                }
            }
            
            // 2차: 스트리밍으로 정확한 크기 계산
            println("DEBUG: Using streaming method to calculate file size")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                var totalBytes = 0L
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                }
                
                println("DEBUG: File size from streaming: $totalBytes bytes")
                CustomResult.Success(totalBytes)
            } ?: CustomResult.Failure(
                StreamingValidationError.FileNotAccessible(uri)
            )
            
        } catch (e: SecurityException) {
            CustomResult.Failure(StreamingValidationError.FileNotAccessible(uri, e))
        } catch (e: IOException) {
            CustomResult.Failure(StreamingValidationError.FileSizeDetectionFailed(uri, e))
        } catch (e: Exception) {
            CustomResult.Failure(StreamingValidationError.FileSizeDetectionFailed(uri, e))
        }
    }

    /**
     * MIME 타입을 확인합니다 (확장자 기반 + 매직 넘버 검증)
     */
    suspend fun detectMimeTypeSecurely(uri: Uri): CustomResult<String, StreamingValidationError> = withContext(Dispatchers.IO) {
        try {
            // 1차: ContentResolver로 MIME 타입 확인
            val resolverMimeType = context.contentResolver.getType(uri)
            
            // 2차: 확장자 기반 MIME 타입 확인
            val extensionMimeType = uri.toString().let { uriString ->
                val extension = MimeTypeMap.getFileExtensionFromUrl(uriString)?.lowercase()
                extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            }
            
            val detectedMimeType = resolverMimeType ?: extensionMimeType
            
            if (detectedMimeType.isNullOrBlank()) {
                return@withContext CustomResult.Failure(
                    StreamingValidationError.MimeTypeDetectionFailed(uri)
                )
            }
            
            // 3차: 매직 넘버로 검증 (보안을 위해)
            val magicNumberValid = validateMagicNumber(uri, detectedMimeType)
            
            if (!magicNumberValid) {
                println("WARNING: Magic number validation failed for MIME type: $detectedMimeType")
                // 매직 넘버가 일치하지 않으면 일반 파일로 처리
                return@withContext CustomResult.Success("application/octet-stream")
            }
            
            CustomResult.Success(detectedMimeType)
            
        } catch (e: Exception) {
            CustomResult.Failure(StreamingValidationError.MimeTypeDetectionFailed(uri))
        }
    }

    /**
     * 파일 이름을 안전하게 추출합니다.
     */
    suspend fun extractFileName(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // 1차: ContentResolver를 통한 파일명 추출
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val fileName = cursor.getString(nameIndex)
                        if (!fileName.isNullOrBlank()) {
                            return@withContext fileName
                        }
                    }
                }
            }
            
            // 2차: URI 경로에서 파일명 추출
            uri.lastPathSegment?.takeIf { it.isNotBlank() }
            
        } catch (e: Exception) {
            // 파일명 추출 실패시 null 반환
            null
        }
    }

    /**
     * 매직 넘버를 사용하여 파일 내용을 검증합니다.
     */
    private suspend fun validateMagicNumber(uri: Uri, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val signatures = FILE_SIGNATURES[mimeType] ?: return@withContext true // 시그니처가 없으면 통과
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val maxSignatureLength = signatures.maxOfOrNull { it.size } ?: 16
                val buffer = ByteArray(maxSignatureLength)
                val bytesRead = inputStream.read(buffer)
                
                if (bytesRead < 4) {
                    return@withContext false // 파일이 너무 작음
                }
                
                signatures.any { signature ->
                    if (signature.size <= bytesRead) {
                        buffer.take(signature.size).toByteArray().contentEquals(signature)
                    } else {
                        false
                    }
                }
            } ?: false
            
        } catch (e: Exception) {
            println("ERROR: Magic number validation failed: ${e.message}")
            false
        }
    }

    /**
     * 종합적인 스트리밍 파일 검증을 수행합니다.
     */
    suspend fun validateStreamingFile(uri: Uri): CustomResult<StreamingValidationResult, StreamingValidationError> {
        return try {
            // 파일 크기 확인
            val fileSizeResult = getAccurateFileSize(uri)
            val fileSize = when (fileSizeResult) {
                is CustomResult.Success -> fileSizeResult.data
                is CustomResult.Failure -> return CustomResult.Failure(fileSizeResult.error)
                is CustomResult.Initial -> return CustomResult.Failure(StreamingValidationError.FileSizeDetectionFailed(uri, Exception("초기 상태")))
                is CustomResult.Loading -> return CustomResult.Failure(StreamingValidationError.FileSizeDetectionFailed(uri, Exception("로딩 중")))
                is CustomResult.Progress -> return CustomResult.Failure(StreamingValidationError.FileSizeDetectionFailed(uri, Exception("진행 중")))
            }
            
            // MIME 타입 확인
            val mimeTypeResult = detectMimeTypeSecurely(uri)
            val mimeType = when (mimeTypeResult) {
                is CustomResult.Success -> mimeTypeResult.data
                is CustomResult.Failure -> return CustomResult.Failure(mimeTypeResult.error)
                is CustomResult.Initial -> return CustomResult.Failure(StreamingValidationError.MimeTypeDetectionFailed(uri))
                is CustomResult.Loading -> return CustomResult.Failure(StreamingValidationError.MimeTypeDetectionFailed(uri))
                is CustomResult.Progress -> return CustomResult.Failure(StreamingValidationError.MimeTypeDetectionFailed(uri))
            }
            
            // 파일명 추출
            val fileName = extractFileName(uri)
            
            // 첨부파일 타입 결정
            val attachmentType = determineAttachmentType(mimeType)
            
            val result = StreamingValidationResult(
                fileSize = fileSize,
                mimeType = mimeType,
                attachmentType = attachmentType,
                fileName = fileName,
                isContentValid = true,
                magicNumberMatch = true
            )
            
            CustomResult.Success(result)
            
        } catch (e: Exception) {
            CustomResult.Failure(StreamingValidationError.FileNotAccessible(uri, e))
        }
    }

    private fun determineAttachmentType(mimeType: String): MessageAttachmentType {
        return when {
            mimeType.startsWith("image/") -> MessageAttachmentType.IMAGE
            mimeType.startsWith("video/") -> MessageAttachmentType.VIDEO
            mimeType.startsWith("audio/") -> MessageAttachmentType.AUDIO
            mimeType.startsWith("text/") || 
            mimeType == "application/pdf" ||
            mimeType.contains("document") ||
            mimeType.contains("sheet") ||
            mimeType.contains("presentation") -> MessageAttachmentType.FILE
            else -> MessageAttachmentType.UNKNOWN
        }
    }
}