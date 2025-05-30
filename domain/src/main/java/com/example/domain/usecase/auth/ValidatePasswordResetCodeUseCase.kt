package com.example.domain.usecase.auth

import javax.inject.Inject

/**
 * 비밀번호 재설정 코드의 형식(예: 길이) 유효성을 검사하는 UseCase.
 * 현재는 코드가 비어있지 않은지만 검사합니다.
 */
class ValidatePasswordResetCodeUseCase @Inject constructor() {
    /**
     * 비밀번호 재설정 코드의 유효성을 검사합니다.
     *
     * @param code 검사할 비밀번호 재설정 코드.
     * @return 코드가 유효하면 true, 그렇지 않으면 false.
     */
    operator fun invoke(code: String): Boolean {
        // TODO: 추후 구체적인 코드 형식 (예: 길이, 숫자/문자 조합 등) 검증 로직 추가 필요
        return code.isNotBlank()
    }
}
