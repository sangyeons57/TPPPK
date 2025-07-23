package com.example.domain.error

import com.example.domain.model.vo.DocumentId
import java.io.IOException

/**
 * 파일 업로드와 관련된 세분화된 에러 타입들
 */
sealed class FileUploadError : Exception() {
    
    // 네트워크 관련 에러
    sealed class NetworkError : FileUploadError() {
        object NoConnection : NetworkError() {
            override val message: String = "인터넷 연결이 없습니다"
            override val isRetryable: Boolean = true
        }
        
        object Timeout : NetworkError() {
            override val message: String = "네트워크 연결 시간이 초과되었습니다"
            override val isRetryable: Boolean = true
        }
        
        data class ServerError(val httpCode: Int) : NetworkError() {
            override val message: String = "서버 오류가 발생했습니다 (코드: $httpCode)"
            override val isRetryable: Boolean = httpCode in 500..599 || httpCode == 429
        }
        
        data class UnknownNetworkError(override val cause: Throwable) : NetworkError() {
            override val message: String = "네트워크 오류: ${cause.message}"
            override val isRetryable: Boolean = true
        }
    }
    
    // 파일 시스템 관련 에러
    sealed class FileSystemError : FileUploadError() {
        data class FileNotFound(val filePath: String) : FileSystemError() {
            override val message: String = "파일을 찾을 수 없습니다: $filePath"
            override val isRetryable: Boolean = false
        }
        
        data class FileAccessDenied(val filePath: String) : FileSystemError() {
            override val message: String = "파일 접근 권한이 없습니다: $filePath"
            override val isRetryable: Boolean = false
        }
        
        data class FileCorrupted(val filePath: String, val reason: String) : FileSystemError() {
            override val message: String = "파일이 손상되었습니다: $reason"
            override val isRetryable: Boolean = false
        }
        
        data class InsufficientStorage(val requiredSpace: Long, val availableSpace: Long) : FileSystemError() {
            override val message: String = "저장 공간이 부족합니다 (필요: ${requiredSpace}MB, 사용 가능: ${availableSpace}MB)"
            override val isRetryable: Boolean = false
        }
    }
    
    // 검증 관련 에러
    sealed class ValidationError : FileUploadError() {
        data class FileSizeExceeded(val maxSize: Long, val actualSize: Long) : ValidationError() {
            override val message: String = "파일 크기가 너무 큽니다 (최대: ${maxSize}MB, 실제: ${actualSize}MB)"
            override val isRetryable: Boolean = false
        }
        
        data class UnsupportedFileType(val fileType: String) : ValidationError() {
            override val message: String = "지원하지 않는 파일 형식입니다: $fileType"
            override val isRetryable: Boolean = false
        }
        
        data class InvalidFileName(val fileName: String, val reason: String) : ValidationError() {
            override val message: String = "잘못된 파일명입니다: $reason"
            override val isRetryable: Boolean = false
        }
        
        data class FileContentMismatch(val expectedType: String, val actualType: String) : ValidationError() {
            override val message: String = "파일 내용이 확장자와 일치하지 않습니다 (예상: $expectedType, 실제: $actualType)"
            override val isRetryable: Boolean = false
        }
    }
    
    // Firebase 관련 에러
    sealed class FirebaseError : FileUploadError() {
        object AuthenticationFailed : FirebaseError() {
            override val message: String = "Firebase 인증에 실패했습니다"
            override val isRetryable: Boolean = false
        }
        
        object QuotaExceeded : FirebaseError() {
            override val message: String = "Firebase Storage 할당량을 초과했습니다"
            override val isRetryable: Boolean = false
        }
        
        data class PermissionDenied(val path: String) : FirebaseError() {
            override val message: String = "Firebase Storage 접근 권한이 없습니다: $path"
            override val isRetryable: Boolean = false
        }
        
        data class FirebaseUnavailable(override val cause: Throwable?) : FirebaseError() {
            override val message: String = "Firebase 서비스를 사용할 수 없습니다"
            override val isRetryable: Boolean = true
        }
    }
    
    // 동시성 관련 에러
    sealed class ConcurrencyError : FileUploadError() {
        data class UploadInProgress(val attachmentId: DocumentId) : ConcurrencyError() {
            override val message: String = "해당 파일이 이미 업로드 중입니다"
            override val isRetryable: Boolean = false
        }
        
        data class UploadCancelled(val attachmentId: DocumentId, val reason: String) : ConcurrencyError() {
            override val message: String = "업로드가 취소되었습니다: $reason"
            override val isRetryable: Boolean = true
        }
        
        data class StateConflict(val currentState: String, val requestedOperation: String) : ConcurrencyError() {
            override val message: String = "현재 상태($currentState)에서 $requestedOperation 작업을 수행할 수 없습니다"
            override val isRetryable: Boolean = false
        }
    }
    
    // 압축 관련 에러
    sealed class CompressionError : FileUploadError() {
        data class CompressionFailed(val reason: String) : CompressionError() {
            override val message: String = "파일 압축에 실패했습니다: $reason"
            override val isRetryable: Boolean = true
        }
        
        object UnsupportedImageFormat : CompressionError() {
            override val message: String = "지원하지 않는 이미지 형식입니다"
            override val isRetryable: Boolean = false
        }
        
        data class OutOfMemory(val fileSize: Long) : CompressionError() {
            override val message: String = "메모리 부족으로 압축을 수행할 수 없습니다 (파일 크기: ${fileSize}MB)"
            override val isRetryable: Boolean = true // 메모리 해제 후 재시도 가능
        }
    }
    
    // 일반적인 에러
    sealed class GenericError : FileUploadError() {
        data class UnexpectedError(override val cause: Throwable) : GenericError() {
            override val message: String = "예기치 않은 오류가 발생했습니다: ${cause.message}"
            override val isRetryable: Boolean = true
        }
        
        data class ConfigurationError(val reason: String) : GenericError() {
            override val message: String = "설정 오류: $reason"
            override val isRetryable: Boolean = false
        }
        
        data class ServiceUnavailable(val serviceName: String) : GenericError() {
            override val message: String = "$serviceName 서비스를 사용할 수 없습니다"
            override val isRetryable: Boolean = true
        }
    }
    
    /**
     * 에러가 재시도 가능한지 확인합니다.
     */
    open val isRetryable: Boolean
        get() = when (this) {
            is NetworkError.NoConnection -> true
            is NetworkError.Timeout -> true
            is NetworkError.ServerError -> isRetryable
            is NetworkError.UnknownNetworkError -> true
            
            is FileSystemError.FileNotFound -> false
            is FileSystemError.FileAccessDenied -> false
            is FileSystemError.FileCorrupted -> false
            is FileSystemError.InsufficientStorage -> false
            
            is ValidationError.FileSizeExceeded -> false
            is ValidationError.UnsupportedFileType -> false
            is ValidationError.InvalidFileName -> false
            is ValidationError.FileContentMismatch -> false
            
            is FirebaseError.AuthenticationFailed -> false
            is FirebaseError.QuotaExceeded -> false
            is FirebaseError.PermissionDenied -> false
            is FirebaseError.FirebaseUnavailable -> true
            
            is ConcurrencyError.UploadInProgress -> false
            is ConcurrencyError.UploadCancelled -> true
            is ConcurrencyError.StateConflict -> false
            
            is CompressionError.CompressionFailed -> true
            is CompressionError.UnsupportedImageFormat -> false
            is CompressionError.OutOfMemory -> true
            
            is GenericError.UnexpectedError -> true
            is GenericError.ConfigurationError -> false
            is GenericError.ServiceUnavailable -> true
        }
    
    /**
     * 에러의 심각도를 반환합니다.
     */
    val severity: ErrorSeverity
        get() = when (this) {
            is NetworkError.NoConnection -> ErrorSeverity.HIGH
            is NetworkError.Timeout -> ErrorSeverity.MEDIUM
            is NetworkError.ServerError -> if (httpCode >= 500) ErrorSeverity.HIGH else ErrorSeverity.MEDIUM
            is NetworkError.UnknownNetworkError -> ErrorSeverity.MEDIUM
            
            is FileSystemError -> ErrorSeverity.HIGH
            is ValidationError -> ErrorSeverity.LOW
            
            is FirebaseError.AuthenticationFailed -> ErrorSeverity.HIGH
            is FirebaseError.QuotaExceeded -> ErrorSeverity.HIGH
            is FirebaseError.PermissionDenied -> ErrorSeverity.HIGH
            is FirebaseError.FirebaseUnavailable -> ErrorSeverity.HIGH
            
            is ConcurrencyError.UploadInProgress -> ErrorSeverity.LOW
            is ConcurrencyError.UploadCancelled -> ErrorSeverity.LOW
            is ConcurrencyError.StateConflict -> ErrorSeverity.MEDIUM
            
            is CompressionError.OutOfMemory -> ErrorSeverity.HIGH
            is CompressionError -> ErrorSeverity.MEDIUM
            
            is GenericError.ConfigurationError -> ErrorSeverity.HIGH
            is GenericError -> ErrorSeverity.MEDIUM
        }
    
    /**
     * 사용자에게 표시할 친화적인 메시지를 반환합니다.
     */
    val userFriendlyMessage: String
        get() = when (this) {
            is NetworkError.NoConnection -> "인터넷 연결을 확인해주세요"
            is NetworkError.Timeout -> "네트워크가 불안정합니다. 잠시 후 다시 시도해주세요"
            is NetworkError.ServerError -> if (httpCode == 429) "요청이 너무 많습니다. 잠시 후 다시 시도해주세요" else "서버에 일시적인 문제가 있습니다"
            is NetworkError.UnknownNetworkError -> "네트워크 오류가 발생했습니다"
            
            is FileSystemError.FileNotFound -> "선택한 파일을 찾을 수 없습니다"
            is FileSystemError.FileAccessDenied -> "파일 접근 권한이 필요합니다"
            is FileSystemError.FileCorrupted -> "파일이 손상되었습니다. 다른 파일을 선택해주세요"
            is FileSystemError.InsufficientStorage -> "저장 공간이 부족합니다"
            
            is ValidationError.FileSizeExceeded -> "파일 크기가 너무 큽니다. 더 작은 파일을 선택해주세요"
            is ValidationError.UnsupportedFileType -> "지원하지 않는 파일 형식입니다"
            is ValidationError.InvalidFileName -> "올바른 파일명이 아닙니다"
            is ValidationError.FileContentMismatch -> "파일 내용과 확장자가 일치하지 않습니다"
            
            is FirebaseError.AuthenticationFailed -> "인증에 실패했습니다. 다시 로그인해주세요"
            is FirebaseError.QuotaExceeded -> "업로드 한도를 초과했습니다"
            is FirebaseError.PermissionDenied -> "업로드 권한이 없습니다"
            is FirebaseError.FirebaseUnavailable -> "서비스를 일시적으로 사용할 수 없습니다"
            
            is ConcurrencyError.UploadInProgress -> "파일이 이미 업로드 중입니다"
            is ConcurrencyError.UploadCancelled -> "업로드가 취소되었습니다"
            is ConcurrencyError.StateConflict -> "현재 작업을 수행할 수 없습니다"
            
            is CompressionError.CompressionFailed -> "파일 처리 중 오류가 발생했습니다"
            is CompressionError.UnsupportedImageFormat -> "지원하지 않는 이미지 형식입니다"
            is CompressionError.OutOfMemory -> "파일이 너무 커서 처리할 수 없습니다"
            
            is GenericError.UnexpectedError -> "예기치 않은 오류가 발생했습니다"
            is GenericError.ConfigurationError -> "앱 설정에 문제가 있습니다"
            is GenericError.ServiceUnavailable -> "서비스를 일시적으로 사용할 수 없습니다"
        }
}

/**
 * 에러의 심각도
 */
enum class ErrorSeverity {
    LOW,    // 사용자 조치로 쉽게 해결 가능
    MEDIUM, // 재시도나 간단한 조치로 해결 가능
    HIGH    // 시스템적 문제로 사용자가 해결하기 어려움
}