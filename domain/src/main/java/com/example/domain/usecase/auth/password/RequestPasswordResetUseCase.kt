package com.example.domain.usecase.auth.password

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.AuthRepository
import javax.inject.Inject

/**
 * 입력된 이메일로 비밀번호 재설정 이메일 발송을 요청하는 UseCase.
 * (기존 RequestPasswordResetUseCase 활용)
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 입력된 이메일로 비밀번호 재설정 이메일 발송을 요청합니다.
     *
     * @param email 비밀번호 재설정을 요청할 이메일 주소.
     * @return 성공 시 CustomResult.Success(Unit), 실패 시 CustomResult.Error를 반환합니다.
     */
    suspend operator fun invoke(email: String): CustomResult<Unit, Exception> {
        return authRepository.requestPasswordResetCode(email)
    }
}