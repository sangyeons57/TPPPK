package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 사용자 프로필 정보를 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 사용자 프로필 정보를 업데이트합니다.
     *
     * @param params 업데이트할 사용자 정보 매개변수
     * @return 성공 시 업데이트된 User 객체, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(params: UpdateUserProfileParams): CustomResult<User, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        return when (sessionResult) {
            is CustomResult.Success -> {
                // User 객체 생성
                val user = User(
                    name = params.name,
                    profileImageUrl = params.profileImageUrl,
                    memo = params.memo
                )
                
                // Repository 호출
                userRepository.updateUserProfile(
                    userId = sessionResult.data.userId,
                    user = user,
                    localImageUri = params.localImageUri
                )
            }
            else -> CustomResult.Failure(Exception("Failed to get current user session"))
        }
    }
    
    /**
     * 사용자 프로필 정보를 업데이트합니다.
     * 
     * @param user 업데이트할 사용자 정보
     * @return 성공 시 업데이트된 User 객체, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(user: User): CustomResult<User, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        return when (sessionResult) {
            is CustomResult.Success -> userRepository.updateUserProfile(sessionResult.data.userId, user)
            else -> CustomResult.Failure(Exception("Failed to get current user session"))
        }
    }
}
