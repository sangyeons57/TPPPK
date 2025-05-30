package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.UserRepository
import javax.inject.Inject

class GetUserProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): CustomResult<String, Exception> { // Returns nullable String for the image URL
        return userRepository.getUserProfileImageUrl(userId)
    }
}
