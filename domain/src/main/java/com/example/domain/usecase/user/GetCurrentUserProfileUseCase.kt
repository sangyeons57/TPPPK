package com.example.domain.usecase.user

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.Result

/**
 * 현재 로그인한 사용자의 프로필 정보를 가져오는 UseCase
 * 
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class GetCurrentUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 현재 로그인한 사용자의 프로필 정보를 Flow로 가져옵니다.
     * 실시간으로 업데이트되는 사용자 정보를 반환합니다.
     *
     * @return 사용자 정보를 포함한 Flow<Result<User?>>
     */
    operator fun invoke(): Flow<Result<User?>> {
        return userRepository.getCurrentUserProfile()
    }
} 