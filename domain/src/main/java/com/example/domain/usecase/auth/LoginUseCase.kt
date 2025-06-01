package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 이메일과 비밀번호로 로그인을 시도하고 사용자 세션 정보를 반환하는 UseCase.
 * (기존 LoginUseCase를 수정함)
 *
 * @property authRepository 인증 관련 기능을 제공하는 Repository.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 이메일과 비밀번호를 이용하여 사용자 로그인을 시도합니다.
     *
     * @param credentials EmailPasswordCredentials 객체 (email, password 포함).
     * @return 성공 시 UserSession이 포함된 CustomResult.Success, 실패 시 CustomResult.Error.
     */
    suspend operator fun invoke(email : String, password : String): CustomResult<UserSession, Exception> {
        return authRepository.login(email, password)
    }
}