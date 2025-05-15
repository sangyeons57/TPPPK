package com.example.domain.usecase.user

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 현재 사용자의 정보를 Stream으로 조회하는 UseCase입니다.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 현재 로그인된 사용자의 정보를 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    operator fun invoke(): Flow<User> {
        return userRepository.getCurrentUserProfileStream()
    }

    /**
     * 특정 사용자의 정보를 Flow로 반환합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 Flow
     */
    operator fun invoke(userId: String): Flow<User> {
        return userRepository.getUserProfileStream(userId)
    }
} 