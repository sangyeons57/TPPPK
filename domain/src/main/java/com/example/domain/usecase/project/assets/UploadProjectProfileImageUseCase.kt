package com.example.domain.usecase.project.assets

import android.hardware.camera2.CameraExtensionSession.ExtensionCaptureCallback
import android.net.Uri
import com.example.core_common.constants.FirebaseStorageConstants
import com.example.core_common.result.CustomResult
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
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
    suspend operator fun invoke(projectId: DocumentId, imageUri: Uri?, fileExtension: String?): CustomResult<Unit, Exception> {
        TODO("Not yet implemented[Firebase FUnction을 통해 구현]")
    }
}
