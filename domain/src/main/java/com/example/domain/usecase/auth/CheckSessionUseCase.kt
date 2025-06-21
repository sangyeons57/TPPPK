package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.repository.base.AuthRepository
import javax.inject.Inject

/**
 * 앱 시작 시 현재 유효한 세션이 있는지 확인하는 UseCase.
 * 사용자의 로그인 상태를 확인하고 세션 정보를 가져옵니다.
 *
 * @property authRepository 인증 관련 기능을 제공하는 Repository.
 */
class CheckSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 앱 시작 시 현재 유효한 사용자 세션이 있는지 확인합니다.
     * Firebase의 인증 상태를 확인하고 세션 정보를 가져옵니다.
     *
     * @return 유효한 세션이 있으면 UserSession이 포함된 CustomResult.Success,
     *         없으면 CustomResult.Success(null), 오류 발생 시 CustomResult.Failure.
     */
    suspend operator fun invoke(): CustomResult<UserSession, Exception> {
        return authRepository.getCurrentUserSession()
    }
}