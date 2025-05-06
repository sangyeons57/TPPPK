package com.example.domain.usecase.user

import android.net.Uri
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 프로필 이미지를 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 */
class UpdateProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 사용자의 프로필 이미지를 업로드하고 업데이트합니다.
     *
     * @param imageUri 업로드할 이미지의 URI
     * @return 성공 시 이미지 URL이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(imageUri: Uri): Result<String?> {
        return userRepository.updateProfileImage(imageUri)
    }
} 