package com.example.domain.usecase.project.core

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectsWrapperRepository
import javax.inject.Inject

/**
 * ProjectsWrapper를 삭제하는 UseCase
 * 프로젝트가 삭제되거나 존재하지 않을 때 사용자의 ProjectsWrapper를 정리하는 용도
 */
interface DeleteProjectsWrapperUseCase {
    /**
     * 지정된 프로젝트 ID의 ProjectsWrapper를 삭제합니다.
     * @param projectId 삭제할 프로젝트 ID
     * @return CustomResult<Unit, Exception> 삭제 결과
     */
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Unit, Exception>
}

/**
 * DeleteProjectsWrapperUseCase 구현체
 */
class DeleteProjectsWrapperUseCaseImpl @Inject constructor(
    private val projectsWrapperRepository: ProjectsWrapperRepository
) : DeleteProjectsWrapperUseCase {

    override suspend fun invoke(projectId: DocumentId): CustomResult<Unit, Exception> {
        return try {
            Log.d("DeleteProjectsWrapperUseCase", "Deleting ProjectsWrapper for projectId: ${projectId.value}")
            
            val result = projectsWrapperRepository.delete(projectId)
            
            when (result) {
                is CustomResult.Success -> {
                    Log.d("DeleteProjectsWrapperUseCase", "Successfully deleted ProjectsWrapper for projectId: ${projectId.value}")
                    CustomResult.Success(Unit)
                }
                is CustomResult.Failure -> {
                    Log.w("DeleteProjectsWrapperUseCase", "Failed to delete ProjectsWrapper for projectId: ${projectId.value}, error: ${result.error}")
                    result
                }
                else -> {
                    val error = Exception("Unexpected result type: $result")
                    Log.e("DeleteProjectsWrapperUseCase", "Unexpected result when deleting ProjectsWrapper for projectId: ${projectId.value}", error)
                    CustomResult.Failure(error)
                }
            }
        } catch (e: Exception) {
            Log.e("DeleteProjectsWrapperUseCase", "Exception when deleting ProjectsWrapper for projectId: ${projectId.value}", e)
            CustomResult.Failure(e)
        }
    }
} 