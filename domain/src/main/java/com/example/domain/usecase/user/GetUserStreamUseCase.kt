package com.example.domain.usecase.user

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 사용자의 정보를 실시간 스트림으로 조회하는 UseCase입니다.
 */
class GetUserStreamUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 특정 사용자의 정보를 실시간 Flow로 반환합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 Flow
     */
    operator fun invoke(userId: String): Flow<Result<User>> {
        return userRepository.getUserStream(userId)
    }
} 