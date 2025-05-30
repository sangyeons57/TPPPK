package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 현재 사용자의 정보를 조회하는 UseCase입니다.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 현재 로그인된 사용자의 정보를 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    suspend operator fun invoke(): Flow<CustomResult<User, Exception>> {
        val currentUserSession = authRepository.getCurrentUserSession()
        if (currentUserSession is CustomResult.Success) {
            return userRepository.getUserStream(currentUserSession.data.userId)
        }
        return flowOf(CustomResult.Failure(Exception("로그인이 필요합니다.")))
    }
} 