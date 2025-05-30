package com.example.domain.usecase.auth

import javax.inject.Inject

/**
 * 비밀번호 형식(최소 길이 등) 유효성을 검사하는 UseCase.
 */
class ValidatePasswordFormatUseCase @Inject constructor() {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8 // 예시: 최소 8자
    }

    /**
     * 비밀번호 형식이 유효한지 검사합니다.
     * 현재는 최소 길이만 검사합니다.
     *
     * @param password 검사할 비밀번호 문자열.
     * @return 비밀번호 형식이 유효하면 true, 그렇지 않으면 false.
     */
    operator fun invoke(password: String): Boolean {
        if (password.isBlank()) {
            return false
        }
        return password.length >= MIN_PASSWORD_LENGTH
    }
}
