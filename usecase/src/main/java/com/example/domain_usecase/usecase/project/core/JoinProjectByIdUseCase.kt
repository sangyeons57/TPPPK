package com.example.domain_usecase.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.ProjectRepository
import javax.inject.Inject

/**
 * projectId를 사용하여 프로젝트에 참여하는 UseCase
 */
class JoinProjectByIdUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String): CustomResult<String, Exception> {
        if (projectId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("프로젝트 ID가 비어있습니다."))
        }

        return when (val result = projectRepository.joinProject(DocumentId.from(projectId))) {
            is CustomResult.Success -> {
                val id = result.data["projectId"] as? String
                if (id != null) CustomResult.Success(id)
                else CustomResult.Failure(Exception("프로젝트 참여 응답에서 프로젝트 ID를 찾을 수 없습니다."))
            }

            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}

