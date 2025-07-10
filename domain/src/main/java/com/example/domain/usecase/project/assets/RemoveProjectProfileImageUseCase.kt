package com.example.domain.usecase.project.assets

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 프로젝트 프로필 이미지를 제거하는 UseCase 인터페이스
 */
interface RemoveProjectProfileImageUseCase {
    /**
     * 프로젝트의 프로필 이미지를 제거합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 성공 시 성공 메시지가 포함된 CustomResult.Success, 실패 시 Exception이 포함된 CustomResult.Failure
     */
    suspend operator fun invoke(projectId: DocumentId): CustomResult<String, Exception>
}

/**
 * 프로젝트 프로필 이미지를 제거하는 UseCase 구현체
 *
 * @property projectRepository 프로젝트 정보 관련 기능을 제공하는 Repository
 */
class RemoveProjectProfileImageUseCaseImpl @Inject constructor(
    private val projectRepository: ProjectRepository
) : RemoveProjectProfileImageUseCase {
    
    /**
     * 프로젝트의 프로필 이미지를 제거합니다.
     * Firebase Functions를 통해 서버에서 프로젝트 프로필 이미지를 삭제합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 성공 시 성공 메시지가 포함된 CustomResult.Success, 실패 시 Exception이 포함된 CustomResult.Failure
     */
    override suspend operator fun invoke(projectId: DocumentId): CustomResult<String, Exception> {
        return try {
            // Firebase Functions를 통해 프로젝트 프로필 이미지 삭제
            val result = projectRepository.removeProfileImage(projectId)
            
            when (result) {
                is CustomResult.Success -> {
                    CustomResult.Success("Project profile image removed successfully")
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error occurred during project profile image removal"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}