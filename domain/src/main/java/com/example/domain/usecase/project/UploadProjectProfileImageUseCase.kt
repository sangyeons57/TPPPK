package com.example.domain.usecase.project

import android.net.Uri
import com.example.core_common.constants.FirebaseStorageConstants
import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.base.ProjectRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * 프로젝트 프로필 이미지를 업로드하고 Firestore에 해당 URL을 업데이트하는 유스케이스입니다.
 * 이미지가 null이면 기존 프로필 이미지를 제거(URL을 null로 설정)합니다.
 */
class UploadProjectProfileImageUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val mediaRepository: MediaRepository
) {

    /**
     * 유스케이스를 실행합니다.
     *
     * @param projectId 프로필 이미지를 업데이트할 프로젝트의 ID.
     * @param imageUri 업로드할 새 프로필 이미지의 Uri. null일 경우 기존 이미지를 제거합니다.
     * @param fileExtension 업로드할 파일의 확장자 (예: "jpg", "png"). `imageUri`가 null이 아니면 필수입니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    suspend operator fun invoke(projectId: String, imageUri: Uri?, fileExtension: String?): CustomResult<Unit, Exception> {
        return try {
            if (imageUri == null) {
                // 이미지 URI가 null이면, 기존 이미지 URL을 null로 설정하여 제거
                // 먼저 현재 프로젝트 정보를 가져와서 기존 이미지 URL을 확인 (삭제를 위해)
                val projectDetailsResult = projectRepository.getProjectDetailsStream(projectId).first() // Flow에서 첫 번째 값만 가져옴
                if (projectDetailsResult is CustomResult.Success) {
                    val oldImageUrl = projectDetailsResult.data.imageUrl
                    if (oldImageUrl != null && oldImageUrl.isNotEmpty()) {
                        mediaRepository.deleteFile(oldImageUrl) // 기존 이미지 삭제 시도 (실패해도 계속 진행)
                    }
                }
                projectRepository.updateProjectProfileImageUrl(projectId, null)
            } else {
                // imageUri is not null, so fileExtension is required.
                if (fileExtension == null || fileExtension.isBlank()) {
                    return CustomResult.Failure(IllegalArgumentException("File extension is required when imageUri is provided."))
                }

                // 새 이미지 업로드 및 URL 업데이트
                // 1. 기존 이미지 URL 가져오기 (삭제 목적)
                var oldImageUrl: String? = null
                val projectDetailsResult = projectRepository.getProjectDetailsStream(projectId).first()
                if (projectDetailsResult is CustomResult.Success) {
                    oldImageUrl = projectDetailsResult.data.imageUrl
                }

                // 2. 새 이미지 업로드
                val actualExtension = fileExtension.removePrefix(".") // Ensure no leading dot
                val fileName = "${UUID.randomUUID()}.$actualExtension"
                val storagePath = FirebaseStorageConstants.getProjectProfileImagePath(projectId, fileName)
                
                val uploadResult = mediaRepository.uploadFile(uri = imageUri, storagePath = storagePath)

                if (uploadResult is CustomResult.Success) {
                    val newImageUrl = uploadResult.data

                    // 3. Firestore에 새 이미지 URL 업데이트
                    val updateDbResult = projectRepository.updateProjectProfileImageUrl(projectId, newImageUrl)
                    if (updateDbResult is CustomResult.Failure) {
                        // DB 업데이트 실패 시, 업로드된 새 이미지도 롤백(삭제)하는 것이 좋음
                        mediaRepository.deleteFile(newImageUrl)
                        return CustomResult.Failure(updateDbResult.error)
                    }

                    // 4. DB 업데이트 성공 후, 기존 이미지 삭제 (존재했다면)
                    if (!oldImageUrl.isNullOrEmpty() && oldImageUrl != newImageUrl) {
                        mediaRepository.deleteFile(oldImageUrl) // 실패해도 로깅만 하고 무시 가능
                    }
                    CustomResult.Success(Unit)
                } else if (uploadResult is CustomResult.Failure) {
                    CustomResult.Failure(uploadResult.error)
                } else {
                    // 업로드 실패
                    CustomResult.Failure(Exception("unknown error"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
