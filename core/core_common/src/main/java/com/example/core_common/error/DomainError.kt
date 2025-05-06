package com.example.core_common.error

/**
 * 애플리케이션 전체에서 사용되는 도메인 에러의 기본 클래스입니다.
 * 모든 비즈니스 로직 관련 에러는 이 클래스를 상속받아야 합니다.
 */
sealed class DomainError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 네트워크 관련 에러
     */
    sealed class NetworkError protected constructor(
        message: String = "네트워크 오류가 발생했습니다.",
        cause: Throwable? = null
    ) : DomainError(message, cause) {
        
        /**
         * 인터넷 연결 없음
         */
        class NoConnection(
            message: String = "인터넷 연결이 없습니다. 네트워크 상태를 확인해주세요.",
            cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        /**
         * 서버 응답 없음
         */
        class Timeout(
            message: String = "서버 응답이 없습니다. 잠시 후 다시 시도해주세요.",
            cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        /**
         * 서버 에러 (5xx)
         */
        class ServerError(
            message: String = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        companion object {
            /**
             * 일반 네트워크 에러 생성
             */
            fun general(message: String = "네트워크 오류가 발생했습니다.", cause: Throwable? = null): DomainError = 
                NoConnection(message, cause)
        }
    }

    /**
     * 인증 관련 에러
     */
    sealed class AuthError protected constructor(
        message: String = "인증 오류가 발생했습니다.",
        cause: Throwable? = null
    ) : DomainError(message, cause) {
        
        /**
         * 로그인 실패
         */
        class LoginFailed(
            message: String = "로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.",
            cause: Throwable? = null
        ) : AuthError(message, cause)
        
        /**
         * 인증 만료
         */
        class SessionExpired(
            message: String = "로그인 세션이 만료되었습니다. 다시 로그인해주세요.",
            cause: Throwable? = null
        ) : AuthError(message, cause)
        
        /**
         * 인증되지 않은 접근
         */
        class Unauthorized(
            message: String = "접근 권한이 없습니다.",
            cause: Throwable? = null
        ) : AuthError(message, cause)
        
        /**
         * 이메일 형식 오류
         */
        class InvalidEmail(
            message: String = "유효하지 않은 이메일 형식입니다.",
            cause: Throwable? = null
        ) : AuthError(message, cause)
        
        /**
         * 비밀번호 형식 오류
         */
        class InvalidPassword(
            message: String = "비밀번호는 최소 6자 이상이어야 합니다.",
            cause: Throwable? = null
        ) : AuthError(message, cause)
        
        companion object {
            /**
             * 일반 인증 에러 생성
             */
            fun general(message: String = "인증 오류가 발생했습니다.", cause: Throwable? = null): DomainError = 
                LoginFailed(message, cause)
        }
    }

    /**
     * 데이터 관련 에러
     */
    sealed class DataError protected constructor(
        message: String = "데이터 오류가 발생했습니다.",
        cause: Throwable? = null
    ) : DomainError(message, cause) {
        
        /**
         * 데이터 없음
         */
        class NotFound(
            message: String = "요청하신 데이터를 찾을 수 없습니다.",
            cause: Throwable? = null
        ) : DataError(message, cause)
        
        /**
         * 데이터 중복
         */
        class AlreadyExists(
            message: String = "이미 존재하는 데이터입니다.",
            cause: Throwable? = null
        ) : DataError(message, cause)
        
        /**
         * 데이터 유효성 오류
         */
        class ValidationFailed(
            message: String = "데이터 유효성 검사에 실패했습니다.",
            cause: Throwable? = null
        ) : DataError(message, cause)
        
        /**
         * 권한 없음
         */
        class PermissionDenied(
            message: String = "해당 데이터에 대한 권한이 없습니다.",
            cause: Throwable? = null
        ) : DataError(message, cause)
        
        companion object {
            /**
             * 일반 데이터 에러 생성
             */
            fun general(message: String = "데이터 오류가 발생했습니다.", cause: Throwable? = null): DomainError = 
                ValidationFailed(message, cause)
        }
    }

    /**
     * 채팅 관련 에러
     */
    sealed class ChatError protected constructor(
        message: String = "채팅 오류가 발생했습니다.",
        cause: Throwable? = null
    ) : DomainError(message, cause) {
        
        /**
         * 메시지 전송 실패
         */
        class SendFailed(
            message: String = "메시지 전송에 실패했습니다.",
            cause: Throwable? = null
        ) : ChatError(message, cause)
        
        /**
         * 채널 접근 불가
         */
        class ChannelAccessDenied(
            message: String = "채널에 접근할 수 없습니다.",
            cause: Throwable? = null
        ) : ChatError(message, cause)
        
        /**
         * 메시지 내용 유효성 오류
         */
        class InvalidMessageContent(
            message: String = "메시지 내용이 유효하지 않습니다.",
            cause: Throwable? = null
        ) : ChatError(message, cause)
        
        companion object {
            /**
             * 일반 채팅 에러 생성
             */
            fun general(message: String = "채팅 오류가 발생했습니다.", cause: Throwable? = null): DomainError = 
                SendFailed(message, cause)
        }
    }

    /**
     * 일반 에러 (위 카테고리에 속하지 않는 경우)
     */
    class UnknownError(
        message: String = "알 수 없는 오류가 발생했습니다.",
        cause: Throwable? = null
    ) : DomainError(message, cause)
} 