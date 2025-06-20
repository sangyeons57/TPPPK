package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.UserStatus
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 현재 사용자의 상태를 가져오는 UseCase 인터페이스
 */
interface GetCurrentStatusUseCase {
    /**
     * 현재 사용자의 상태를 가져옵니다.
     *
     * @return 성공 시 사용자 상태가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(): Flow<CustomResult<UserStatus, Exception>>
}

/**
 * 현재 사용자의 상태를 가져오는 UseCase 구현체
 * 
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class GetCurrentStatusUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : GetCurrentStatusUseCase {
    /**
     * 현재 사용자의 상태를 가져옵니다.
     *
     * @return 성공 시 사용자 상태가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    override suspend operator fun invoke(): Flow<CustomResult<UserStatus, Exception>> {
        val session = authRepository.getCurrentUserSession()
        return when (session) {
            is CustomResult.Success -> {
                userRepository.observe(session.data.userId).map {
                    if (it is CustomResult.Success) {
                        CustomResult.Success(it.data.userStatus)
                    }  else {
                        CustomResult.Failure(Exception("Unknown error"))
                    }
                }
            }
            else -> flowOf(CustomResult.Failure(Exception("로그인이 필요합니다.")))
        }
    }
} 