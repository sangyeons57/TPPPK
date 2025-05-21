package com.example.domain.usecase.user

import android.net.Uri // Import Android Uri
import com.example.domain.repository.UserRepository
import javax.inject.Inject

class UploadProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(imageUri: Uri): Result<String> { // Returns download URL as String
        return userRepository.uploadProfileImage(imageUri)
    }
}
