package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.repository.base.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get a stream of project lists for the current user.
 */
class GetProjectListStreamUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(): Flow<CustomResult<List<Project>, Exception>> {
        TODO()
        // return projectRepository.getProjectListStream()
    }
}
