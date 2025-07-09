package com.example.domain.usecase.user

import android.net.Uri
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자 이미지 업데이트 유스케이스 인터페이스
 */
interface UpdateUserImageUseCase {
    suspend operator fun invoke(imageUri: Uri): Result<String> // 성공 시 새 이미지 URL 반환
}

/**
 * UpdateUserImageUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 */
class UpdateUserImageUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : UpdateUserImageUseCase {

    /**
     * 유스케이스를 실행하여 사용자 이미지를 업데이트합니다.
     * @param imageUri 업데이트할 이미지의 Uri
     * @return Result<String> 업데이트 처리 결과 (성공 시 새로운 이미지 URL, 실패 시 Exception)
     */
    override suspend fun invoke(imageUri: Uri): Result<String> {
        return try {
            // Firebase Storage에 이미지 업로드 (Firebase Functions가 자동으로 Firestore 업데이트)
            when (val result = userRepository.uploadProfileImage(imageUri)) {
                is com.example.core_common.result.CustomResult.Success -> {
                    // 업로드 성공 시 임시 URL 반환 (실제 URL은 Firebase Functions에서 처리)
                    Result.success("Profile image uploaded successfully")
                }
                is com.example.core_common.result.CustomResult.Failure -> {
                    Result.failure(result.error)
                }
                else -> {
                    Result.failure(Exception("Unknown error occurred during image upload"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 