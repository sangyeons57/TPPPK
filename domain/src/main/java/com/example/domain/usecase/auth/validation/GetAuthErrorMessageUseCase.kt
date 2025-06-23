package com.example.domain.usecase.auth.validation

import com.example.domain.model.ui.enum.AuthErrorType
import javax.inject.Inject

/**
 * 인증 관련 오류 메시지를 제공하는 UseCase
 * Firebase 인증 오류 코드를 사용자가 이해할 수 있는 메시지로 변환합니다.
 */
interface GetAuthErrorMessageUseCase {
    /**
     * 인증 오류 코드에 해당하는 메시지를 반환합니다.
     *
     * @param errorCode 처리할 오류 코드 (문자열)
     * @return 사용자 친화적인 오류 메시지
     */
    operator fun invoke(errorCode: String): String
    
    /**
     * 인증 오류 타입에 해당하는 메시지를 반환합니다.
     *
     * @param errorType 처리할 오류 타입 (AuthErrorType 열거형)
     * @return 사용자 친화적인 오류 메시지
     */
    operator fun invoke(errorType: AuthErrorType): String
    
    /**
     * 예외를 분석하여 적절한 인증 오류 메시지를 반환합니다.
     *
     * @param exception 처리할 예외
     * @return 사용자 친화적인 오류 메시지
     */
    operator fun invoke(exception: Exception): String
    
    /**
     * 로그인 오류에 해당하는 메시지를 반환합니다.
     *
     * @param exception 처리할 예외
     * @return 사용자 친화적인 오류 메시지
     */
    fun getLoginErrorMessage(exception: Exception): String
    
    /**
     * 회원가입 오류에 해당하는 메시지를 반환합니다.
     *
     * @param exception 처리할 예외
     * @return 사용자 친화적인 오류 메시지
     */
    fun getSignUpErrorMessage(exception: Exception): String
}

/**
 * GetAuthErrorMessageUseCase의 구현체
 * Firebase 인증 오류 코드를 사용자 친화적인 메시지로 변환합니다.
 */
class GetAuthErrorMessageUseCaseImpl @Inject constructor() : GetAuthErrorMessageUseCase {
    
    /**
     * 인증 오류 코드에 해당하는 메시지를 반환합니다.
     *
     * @param errorCode 처리할 오류 코드
     * @return 사용자 친화적인 오류 메시지
     */
    override operator fun invoke(errorCode: String): String {
        // 오류 코드를 AuthErrorType으로 변환
        val errorType = when (errorCode) {
            "ERROR_INVALID_EMAIL" -> AuthErrorType.INVALID_EMAIL
            "ERROR_WRONG_PASSWORD" -> AuthErrorType.WRONG_PASSWORD
            "ERROR_USER_NOT_FOUND" -> AuthErrorType.USER_NOT_FOUND
            "ERROR_USER_DISABLED" -> AuthErrorType.USER_DISABLED
            "ERROR_TOO_MANY_REQUESTS" -> AuthErrorType.TOO_MANY_REQUESTS
            "ERROR_OPERATION_NOT_ALLOWED" -> AuthErrorType.OPERATION_NOT_ALLOWED
            "ERROR_WEAK_PASSWORD" -> AuthErrorType.WEAK_PASSWORD
            "ERROR_EMAIL_ALREADY_IN_USE" -> AuthErrorType.EMAIL_ALREADY_IN_USE
            else -> AuthErrorType.UNKNOWN_ERROR
        }
        
        return invoke(errorType)
    }
    
    /**
     * 인증 오류 타입에 해당하는 메시지를 반환합니다.
     *
     * @param errorType 처리할 오류 타입 (AuthErrorType 열거형)
     * @return 사용자 친화적인 오류 메시지
     */
    override operator fun invoke(errorType: AuthErrorType): String {
        return when (errorType) {
            AuthErrorType.INVALID_EMAIL -> "유효하지 않은 이메일 형식입니다."
            AuthErrorType.WRONG_PASSWORD -> "비밀번호가 일치하지 않습니다."
            AuthErrorType.USER_NOT_FOUND -> "등록되지 않은 사용자입니다."
            AuthErrorType.USER_DISABLED -> "사용이 중지된 계정입니다."
            AuthErrorType.TOO_MANY_REQUESTS -> "너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요."
            AuthErrorType.OPERATION_NOT_ALLOWED -> "이메일/비밀번호 로그인이 비활성화되었습니다."
            AuthErrorType.WEAK_PASSWORD -> "비밀번호는 최소 6자리 이상이어야 합니다."
            AuthErrorType.EMAIL_ALREADY_IN_USE -> "이미 사용 중인 이메일입니다."
            AuthErrorType.NETWORK_ERROR -> "네트워크 연결 오류가 발생했습니다. 인터넷 연결을 확인해주세요."
            AuthErrorType.TIMEOUT -> "요청 시간이 초과되었습니다. 다시 시도해주세요."
            AuthErrorType.RESET_PASSWORD_FAILURE -> "비밀번호 재설정 메일 발송 실패했습니다."
            AuthErrorType.UNKNOWN_ERROR -> "알 수 없는 오류가 발생했습니다. 다시 시도해주세요."
        }
    }
    
    /**
     * 예외를 분석하여 적절한 인증 오류 메시지를 반환합니다.
     *
     * @param exception 처리할 예외
     * @return 사용자 친화적인 오류 메시지
     */
    override operator fun invoke(exception: Exception): String {
        // 예외 메시지에서 오류 코드 추출 시도
        val errorMessage = exception.message ?: return invoke(AuthErrorType.UNKNOWN_ERROR)
        
        // Firebase 오류 코드 패턴 추출: [ERROR_CODE]
        val errorCodeRegex = "ERROR_[A-Z_]+".toRegex()
        val matchResult = errorCodeRegex.find(errorMessage)
        
        return if (matchResult != null) {
            invoke(matchResult.value)
        } else {
            // 네트워크 오류 감지
            if (errorMessage.contains("network", ignoreCase = true) || 
                errorMessage.contains("connection", ignoreCase = true)) {
                invoke(AuthErrorType.NETWORK_ERROR)
            } 
            // 시간 초과 오류 감지
            else if (errorMessage.contains("timeout", ignoreCase = true)) {
                invoke(AuthErrorType.TIMEOUT)
            }
            else {
                invoke(AuthErrorType.UNKNOWN_ERROR)
            }
        }
    }
    
    /**
     * 로그인 오류에 해당하는 메시지를 반환합니다.
     *
     * @param exception 처리할 예외
     * @return 사용자 친화적인 오류 메시지
     */
    override fun getLoginErrorMessage(exception: Exception): String {
        return invoke(exception) // 기본 예외 처리 로직 사용
    }
    
    /**
     * 회원가입 오류에 해당하는 메시지를 반환합니다.
     *
     * @param exception 처리할 예외
     * @return 사용자 친화적인 오류 메시지
     */
    override fun getSignUpErrorMessage(exception: Exception): String {
        val message = invoke(exception)
        // 회원가입 관련 특별 오류 처리가 필요하면 여기에 추가
        return message
    }
}
