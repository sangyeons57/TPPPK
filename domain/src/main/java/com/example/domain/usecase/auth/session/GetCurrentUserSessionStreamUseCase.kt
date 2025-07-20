package com.example.domain.usecase.auth.session

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.repository.base.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자의 세션 상태 변경을 실시간으로 관찰하는 UseCase 입니다.
 *
 * 로그인, 로그아웃 등 인증 상태에 변화가 생길 때마다
 * 새로운 UserSession 정보를 담은 Flow를 발행합니다.
 */
class GetCurrentUserSessionStreamUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<CustomResult<UserSession, Exception>> {
        return authRepository.getCurrentUserSessionStream()
    }
}