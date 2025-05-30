package com.example.domain.usecase.user

import android.net.Uri // Import Android Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject

class UploadProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(imageUri: Uri): CustomResult<String, Exception> { // Returns download URL as String
        val session = authRepository.getCurrentUserSession()
        when (session) {
            is CustomResult.Success -> {
                userRepository.updateUserProfileImage(session.data.userId, imageUri)
            }
            else -> {
            }
        }
    }
}
