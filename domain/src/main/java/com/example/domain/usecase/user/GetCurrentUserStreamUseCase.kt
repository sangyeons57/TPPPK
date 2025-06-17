package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 현재 사용자의 정보를 실시간 스트림으로 조회하는 UseCase 인터페이스입니다.
 */
interface GetCurrentUserStreamUseCase {
    /**
     * 현재 로그인된 사용자의 정보를 실시간 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    suspend operator fun invoke(): Flow<CustomResult<User, Exception>>
}

/**
 * 현재 사용자의 정보를 실시간 스트림으로 조회하는 UseCase 구현체입니다.
 */
class GetCurrentUserStreamUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : GetCurrentUserStreamUseCase {
    /**
     * 현재 로그인된 사용자의 정보를 실시간 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    override suspend operator fun invoke(): Flow<CustomResult<User, Exception>> {
        val session = authRepository.getCurrentUserSession()
        return when (session) {
            is CustomResult.Success -> {
                userRepository.observe(session.data.userId)
            }
            else -> {
                flowOf( CustomResult.Failure(Exception("로그인이 필요합니다.")) )
            }
        }
    }
} 