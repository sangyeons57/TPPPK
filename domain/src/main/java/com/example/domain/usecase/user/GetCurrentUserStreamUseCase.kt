package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 현재 사용자의 정보를 실시간 스트림으로 조회하는 UseCase입니다.
 */
class GetCurrentUserStreamUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 현재 로그인된 사용자의 정보를 실시간 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    operator fun invoke(): Flow<CustomResult<User, Exception>> {
        return userRepository.getCurrentUserStream()
    }
} 