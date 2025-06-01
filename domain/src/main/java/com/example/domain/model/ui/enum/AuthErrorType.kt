package com.example.domain.model.ui.enum

/**
 * 인증 관련 오류 유형을 정의하는 열거형
 * UI에서 사용자에게 적절한 오류 메시지를 표시하는 데 사용됩니다.
 */
enum class AuthErrorType {
    /**
     * 유효하지 않은 이메일 형식
     */
    INVALID_EMAIL,
    
    /**
     * 올바르지 않은 비밀번호
     */
    WRONG_PASSWORD,
    
    /**
     * 등록되지 않은 사용자
     */
    USER_NOT_FOUND,
    
    /**
     * 사용이 중지된 계정
     */
    USER_DISABLED,
    
    /**
     * 너무 많은 로그인 시도
     */
    TOO_MANY_REQUESTS,
    
    /**
     * 이메일/비밀번호 로그인 비활성화
     */
    OPERATION_NOT_ALLOWED,
    
    /**
     * 약한 비밀번호 (회원가입)
     */
    WEAK_PASSWORD,
    
    /**
     * 이미 사용 중인 이메일 (회원가입)
     */
    EMAIL_ALREADY_IN_USE,
    
    /**
     * 네트워크 연결 오류
     */
    NETWORK_ERROR,
    
    /**
     * 인증 시간 초과
     */
    TIMEOUT,
    
    /**
     * 비밀번호 재설정 메일 발송 실패
     */
    RESET_PASSWORD_FAILURE,
    
    /**
     * 알 수 없는 오류
     */
    UNKNOWN_ERROR
}
