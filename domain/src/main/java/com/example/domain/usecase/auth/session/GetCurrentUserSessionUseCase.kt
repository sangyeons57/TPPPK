package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.repository.remote.AuthRepository
import javax.inject.Inject

/**
 * 현재 사용자의 기본 세션 정보를 가져오는 UseCase.
 * 토큰이 필요하지 않은 경우 사용합니다.
 *
 * @property authRepository 인증 관련 기능을 제공하는 Repository.
 */
class GetCurrentUserSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 현재 사용자의 기본 세션 정보를 가져옵니다.
     * 토큰은 포함되지 않습니다.
     *
     * @return UserSession이 포함된 CustomResult.Success,
     *         오류 발생 시 CustomResult.Failure.
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): CustomResult<UserSession, Exception> {
        return authRepository.getCurrentUserSession(forceRefresh = forceRefresh)
    }
}