package com.example.domain.usecase.auth.password

import com.example.core_common.result.CustomResult
import javax.inject.Inject

/**
 * 새 비밀번호의 유효성(길이, 특수문자 포함 등)을 검사하는 UseCase.
 */
class ValidateNewPasswordUseCase @Inject constructor() {

    // TODO: 비밀번호 정책이 확정되면 정규식 또는 구체적인 검증 로직으로 변경해야 합니다.
    // 예: val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&*+=?-]).{8,15}$"

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

    /**
     * 새 비밀번호의 유효성을 검사합니다.
     *
     * @param password 검사할 새 비밀번호.
     * @return PasswordValidationResult 객체 (유효성 상태와 메시지 포함).
     */
    operator fun invoke(password: String): CustomResult<Unit, Exception> {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return CustomResult.Failure(IllegalArgumentException("비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 합니다."))
        }
        if (!password.any { it.isDigit() }) {
            return CustomResult.Failure(IllegalArgumentException("비밀번호에는 숫자가 1개 이상 포함되어야 합니다."))
        }
        if (!password.any { it.isLetter() }) {
            return CustomResult.Failure(IllegalArgumentException("비밀번호에는 영문자가 1개 이상 포함되어야 합니다"))
        }
        // TODO: 추후 특수문자 포함 등의 추가적인 검증 로직을 여기에 추가할 수 있습니다.

        return CustomResult.Success(Unit)
    }
}
