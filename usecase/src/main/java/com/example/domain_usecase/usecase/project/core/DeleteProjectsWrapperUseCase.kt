package com.example.domain_usecase.usecase.project.core

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.ProjectsWrapperRepository
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
            Log.d("DeleteProjectsWrapperUseCase", "=== INVOKE START === projectId: ${projectId.value}")
            Log.d("DeleteProjectsWrapperUseCase", "Calling projectsWrapperRepository.delete() for projectId: ${projectId.value}")
            
            val result = projectsWrapperRepository.delete(projectId)
            Log.d("DeleteProjectsWrapperUseCase", "Repository delete result: $result")
            
            when (result) {
                is CustomResult.Success -> {
                    Log.d("DeleteProjectsWrapperUseCase", "✅ Successfully deleted ProjectsWrapper for projectId: ${projectId.value}")
                    Log.d("DeleteProjectsWrapperUseCase", "=== INVOKE END (SUCCESS) ===")
                    CustomResult.Success(Unit)
                }
                is CustomResult.Failure -> {
                    Log.w("DeleteProjectsWrapperUseCase", "❌ Failed to delete ProjectsWrapper for projectId: ${projectId.value}, error: ${result.error}")
                    Log.w("DeleteProjectsWrapperUseCase", "Error details: ${result.error.message}")
                    Log.d("DeleteProjectsWrapperUseCase", "=== INVOKE END (FAILURE) ===")
                    result
                }
                else -> {
                    val error = Exception("Unexpected result type: $result")
                    Log.e("DeleteProjectsWrapperUseCase", "⚠️ Unexpected result when deleting ProjectsWrapper for projectId: ${projectId.value}, result: $result", error)
                    Log.d("DeleteProjectsWrapperUseCase", "=== INVOKE END (UNEXPECTED) ===")
                    CustomResult.Failure(error)
                }
            }
        } catch (e: Exception) {
            Log.e("DeleteProjectsWrapperUseCase", "💥 Exception when deleting ProjectsWrapper for projectId: ${projectId.value}", e)
            Log.e("DeleteProjectsWrapperUseCase", "Exception type: ${e.javaClass.simpleName}")
            Log.e("DeleteProjectsWrapperUseCase", "Exception message: ${e.message}")
            Log.d("DeleteProjectsWrapperUseCase", "=== INVOKE END (EXCEPTION) ===")
            CustomResult.Failure(e)
        }
    }
} 