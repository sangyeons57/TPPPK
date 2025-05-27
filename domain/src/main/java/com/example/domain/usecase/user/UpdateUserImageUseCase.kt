// UserRepository method used:
// interface UserRepository {
//     // ... other methods
//     suspend fun updateUserProfileImage(
//         userId: String,
//         imageInputStream: InputStream,
//         imageMimeType: String
//     ): Result<String?> // Returns new image URL or null
// }
// Note: This UseCase's signature has been changed from (imageUri: Uri) to
// (userId: String, imageInputStream: InputStream, imageMimeType: String)
// to align with UserRepository and keep domain layer free of Android-specific Uri processing.
package com.example.domain.usecase.user

import com.example.domain._repository.UserRepository // Ensure correct import path
import java.io.InputStream
import javax.inject.Inject

/**
 * 사용자 프로필 이미지를 업데이트하는 유스케이스 인터페이스.
 * The responsibility of converting Uri to InputStream lies with the caller (e.g., ViewModel).
 */
interface UpdateUserImageUseCase {
    /**
     * @param userId 업데이트할 사용자의 ID
     * @param imageInputStream 업데이트할 이미지의 InputStream
     * @param imageMimeType 이미지의 MIME 타입 (e.g., "image/jpeg")
     * @return Result<String?> 성공 시 새 이미지 URL (nullable), 실패 시 Exception
     */
    suspend operator fun invoke(userId: String, imageInputStream: InputStream, imageMimeType: String): Result<String?>
}

/**
 * UpdateUserImageUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 */
class UpdateUserImageUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : UpdateUserImageUseCase {

    /**
     * 유스케이스를 실행하여 사용자 프로필 이미지를 업데이트합니다.
     * @param userId 업데이트할 사용자의 ID
     * @param imageInputStream 업데이트할 이미지의 InputStream
     * @param imageMimeType 이미지의 MIME 타입
     * @return Result<String?> 업데이트 처리 결과 (성공 시 새로운 이미지 URL (nullable), 실패 시 Exception)
     */
    override suspend fun invoke(userId: String, imageInputStream: InputStream, imageMimeType: String): Result<String?> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }
        // imageMimeType can be validated further if necessary (e.g., specific supported types)

        return userRepository.updateUserProfileImage(
            userId = userId,
            imageInputStream = imageInputStream,
            imageMimeType = imageMimeType
        )
    }
}