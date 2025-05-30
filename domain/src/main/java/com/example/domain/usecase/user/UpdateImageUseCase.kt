package com.example.domain.usecase.user

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 프로필 이미지를 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 */
class UpdateImageUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 사용자의 프로필 이미지를 업로드하고, 해당 이미지 URL로 사용자 프로필을 업데이트합니다.
     *
     * @param imageUri 업로드할 이미지의 URI
     * @return 성공 시 이미지 URL이 포함된 Result, 실패 시 에러 정보가 포함된 Result (kotlin.Result 사용)
     */
    suspend operator fun invoke(imageUri: Uri): CustomResult<String, Exception> {
        // 1. 현재 사용자 프로필 정보를 가져옵니다.
        val currentUserProfileResult = userRepository.getMyProfile()

        // kotlin.Result의 fold를 사용하여 성공/실패 처리
        return currentUserProfileResult.fold(
            onSuccess = { currentUser ->
                // 2. 이미지를 업로드하고 URL을 받습니다.
                val uploadResult = userRepository.uploadProfileImage(imageUri)

                uploadResult.fold(
                    onSuccess = { newImageUrl ->
                        // 3. 가져온 사용자 이름과 새 이미지 URL로 프로필을 업데이트합니다.
                        val updateProfileResult = userRepository.updateUserProfile(currentUser.name, newImageUrl)

                        updateProfileResult.fold(
                            onSuccess = { Result.success(newImageUrl) }, // 성공 시 새 이미지 URL 반환
                            onFailure = { Result.failure(it) }
                        )
                    },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }
} 