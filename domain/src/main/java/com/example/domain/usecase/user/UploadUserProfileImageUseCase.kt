package com.example.domain.usecase.user

import android.net.Uri
import com.example.core_common.constants.FirebaseStorageConstants
import com.example.core_common.result.CustomResult
import com.example.domain.repository.FileRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * 사용자 프로필 이미지를 업로드하고 Firestore의 사용자 문서에 이미지 URL을 업데이트하는 유스케이스입니다.
 * 이미지가 null이면 기존 이미지 URL을 제거(null로 설정)합니다.
 */
class UploadUserProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository // MediaRepository 대신 FileRepository 사용 (기존 코드 유지)
) {
    /**
     * 유스케이스를 실행합니다.
     *
     * @param userId 프로필 이미지를 업데이트할 사용자의 ID.
     * @param imageUri 업로드할 새 이미지의 Uri. null일 경우 기존 프로필 이미지를 제거(URL을 null로 설정)합니다.
     * @param fileExtension 업로드할 파일의 확장자 (예: "jpg", "png"). `imageUri`가 null이 아니면 필수입니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    suspend operator fun invoke(userId: String, imageUri: Uri?, fileExtension: String?): CustomResult<Unit, Exception> {
        return try {
            if (imageUri == null) {
                // 이미지 URI가 null이면, 기존 이미지 URL을 null로 설정하여 제거
                // 먼저 현재 사용자 정보를 가져와서 기존 이미지 URL을 확인 (삭제를 위해)
                // UserRepository에 getUserStream 또는 유사한 메소드가 있다고 가정합니다.
                // 만약 없다면, 이 부분은 userRepository.updateUserProfileImageUrl(userId, null)만 호출하도록 단순화해야 합니다.
                // 또는 UserRepository에 getImageUrl(userId) 같은 메소드를 추가해야 합니다.
                // 여기서는 getUserStream이 있고 User 객체에 imageUrl이 있다고 가정합니다.
                var oldImageUrl: String? = null
                val userDetailsResult = userRepository.getUserStream(userId).first()
                if (userDetailsResult is CustomResult.Success) {
                    oldImageUrl = userDetailsResult.data?.profileImageUrl
                }
                // else: failed to get user details, proceed to set URL to null, old image deletion might not happen.

                // Attempt to delete the old image from storage if it exists
                if (!oldImageUrl.isNullOrEmpty()) {
                    // Assume FileRepository has a deleteFile method similar to MediaRepository
                    // If not, this part needs adjustment or FileRepository needs an update.
                    // val deleteOldImageResult = fileRepository.deleteFile(oldImageUrl) // TODO: Uncomment if FileRepository.deleteFile(url) exists
                    // Log if deletion failed, but don't block the main operation of setting URL to null.
                    // if (deleteOldImageResult is CustomResult.Failure) {
                    //     // Log error: e.g., Timber.e(deleteOldImageResult.error, "Failed to delete old user profile image: $oldImageUrl")
                    // }
                }

                // Update Firestore to set the image URL to null
                return when (val updateResult = userRepository.updateUserProfileImageUrl(userId, null)) {
                    is CustomResult.Success -> CustomResult.Success(Unit)
                    is CustomResult.Failure -> CustomResult.Failure(updateResult.error)
                    else -> CustomResult.Failure(Exception("Unexpected result from updateUserProfileImageUrl while clearing URL."))
                }
            } else {
                // imageUri is not null, so fileExtension is required.
                if (fileExtension == null || fileExtension.isBlank()) {
                    return CustomResult.Failure(IllegalArgumentException("File extension is required when imageUri is provided."))
                }

                // 1. Get current user details to find the old image URL (for later deletion)
                var oldImageUrl: String? = null
                val currentUserResult = userRepository.getUserStream(userId).first()
                if (currentUserResult is CustomResult.Success) {
                    oldImageUrl = currentUserResult.data?.profileImageUrl
                }
                // else: If fetching user details fails, we might not be able to delete the old image. Proceed with upload.

                // 2. Upload the new image to Firebase Storage
                val actualExtension = fileExtension.removePrefix(".") // Ensure no leading dot
                val newFileName = "${UUID.randomUUID()}.$actualExtension"
                val newImageStoragePath = FirebaseStorageConstants.getUserProfileImagePath(userId, newFileName)

                val uploadResult = fileRepository.uploadFile(newImageStoragePath, imageUri)

                return when (uploadResult) {
                    is CustomResult.Success -> {
                        val newImageUrl = uploadResult.data

                        // 3. Update Firestore with the new image URL
                        val updateFirestoreResult = userRepository.updateUserProfileImageUrl(userId, newImageUrl)

                        when (updateFirestoreResult) {
                            is CustomResult.Success -> {
                                // 4. Firestore updated successfully. Delete the old image from Storage, if it existed and is different.
                                if (!oldImageUrl.isNullOrEmpty() && oldImageUrl != newImageUrl) {
                                    // Assume FileRepository has a deleteFile method.
                                    // val deleteOldResult = fileRepository.deleteFile(oldImageUrl) // TODO: Uncomment if FileRepository.deleteFile(url) exists
                                    // Log if deletion failed, but don't fail the overall operation.
                                    // if (deleteOldResult is CustomResult.Failure) {
                                    //    // Timber.e(deleteOldResult.error, "Failed to delete old user profile image: $oldImageUrl")
                                    // }
                                }
                                CustomResult.Success(Unit)
                            }
                            is CustomResult.Failure -> {
                                // Firestore update failed. Rollback: delete the newly uploaded image from Storage.
                                // val deleteNewUploadedResult = fileRepository.deleteFile(newImageUrl) // TODO: Uncomment if FileRepository.deleteFile(url) exists
                                // Log if rollback deletion failed.
                                // if (deleteNewUploadedResult is CustomResult.Failure) {
                                //    // Timber.e(deleteNewUploadedResult.error, "Failed to delete newly uploaded user image during rollback: $newImageUrl")
                                // }
                                CustomResult.Failure(updateFirestoreResult.error)
                            }
                            else -> {
                                // Also attempt rollback for unexpected states from updateUserProfileImageUrl
                                // fileRepository.deleteFile(newImageUrl) // TODO: Uncomment if FileRepository.deleteFile(url) exists
                                CustomResult.Failure(Exception("Unexpected result from updateUserProfileImageUrl: $updateFirestoreResult"))
                            }
                        }
                    }
                    is CustomResult.Failure -> {
                        // Image upload failed.
                        CustomResult.Failure(uploadResult.error)
                    }
                    else -> {
                        // Handle other CustomResult states from uploadFile if necessary,
                        // though typically it would be Success or Failure.
                        CustomResult.Failure(Exception("Unexpected result state from fileRepository.uploadFile: $uploadResult"))
                    }
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
