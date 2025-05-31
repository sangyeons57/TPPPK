package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject


class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(user: User): CustomResult<Unit, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        return when (sessionResult){
            is CustomResult.Success -> userRepository.updateUserProfile(sessionResult.data.userId, user)
            else -> CustomResult.Failure(Exception("Failed to get current user session"))
        }
    }
}
