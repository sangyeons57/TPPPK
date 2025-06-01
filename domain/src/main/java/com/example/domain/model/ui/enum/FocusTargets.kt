package com.example.domain.model.ui.enum

/**
 * 로그인 폼에서 포커스를 요청할 수 있는 대상 필드를 정의하는 열거형
 */
enum class LoginFormFocusTarget {
    /**
     * 이메일 입력 필드
     */
    EMAIL,
    
    /**
     * 비밀번호 입력 필드
     */
    PASSWORD,
    
    /**
     * 로그인 버튼
     */
    LOGIN_BUTTON
}

/**
 * 회원가입 폼에서 포커스를 요청할 수 있는 대상 필드를 정의하는 열거형
 */
enum class SignUpFormFocusTarget {
    /**
     * 이메일 입력 필드
     */
    EMAIL,
    
    /**
     * 비밀번호 입력 필드
     */
    PASSWORD,
    
    /**
     * 비밀번호 확인 입력 필드
     */
    PASSWORD_CONFIRM,
    
    /**
     * 이름(닉네임) 입력 필드
     */
    NAME,
    
    /**
     * 회원가입 버튼
     */
    SIGNUP_BUTTON,
    
    /**
     * 포커스 요청 없음
     */
    NONE
}
