package com.example.domain.usecase.user

import com.example.domain.model.User // Changed to User
import com.example.domain.repository.UserRepository
import javax.inject.Inject

class GetMyProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<User> { // Changed to User
        return userRepository.getMyProfile()
    }
}
