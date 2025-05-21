package com.example.domain.usecase.user

import com.example.domain.repository.UserRepository
import com.example.domain.model.Result // Using the temporary Result from domain.model
import javax.inject.Inject

// Parameter data class for clarity
data class UpdateUserProfileParams(
    val name: String,
    val profileImageUrl: String? // Nullable if the image is not changed or removed
)

class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(params: UpdateUserProfileParams): Result<Unit> {
        return userRepository.updateUserProfile(name = params.name, profileImageUrl = params.profileImageUrl)
    }
}
