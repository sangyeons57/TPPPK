package com.example.domain.usecase.auth

import android.util.Patterns
import javax.inject.Inject

/**
 * 이메일 주소의 유효성을 검사하는 UseCase.
 *
 * @property emailFormatValidator 이메일 형식 검증을 담당하는 유틸리티 클래스
 */
class ValidateEmailUseCase @Inject constructor() {
    
    /**
     * 이메일 주소의 유효성을 검사합니다.
     *
     * @param email 검사할 이메일 주소
     * @return 이메일이 유효하면 true, 그렇지 않으면 false
     */
    operator fun invoke(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * 이메일 주소의 유효성을 검사하고 오류 메시지를 반환합니다.
     *
     * @param email 검사할 이메일 주소
     * @return 이메일이 유효하면 null, 그렇지 않으면 오류 메시지
     */
    fun validateWithMessage(email: String): String? {
        return when {
            email.isBlank() -> "이메일을 입력해주세요."
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "올바른 이메일 형식이 아닙니다."
            else -> null
        }
    }
}
