package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 프로젝트의 상세 정보를 스트림으로 가져오는 UseCase입니다.
 */
class GetProjectDetailsStreamUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 지정된 ID의 프로젝트 상세 정보를 Flow로 반환합니다.
     *
     * @param projectId 가져올 프로젝트의 ID
     * @return 프로젝트 상세 정보를 포함하는 Flow
     */
    operator fun invoke(projectId: String): Flow<CustomResult<Project, Exception>> {
        return projectRepository.getProjectDetailsStream(projectId)
    }
}
