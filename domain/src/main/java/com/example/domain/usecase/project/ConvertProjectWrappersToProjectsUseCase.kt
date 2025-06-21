package com.example.domain.usecase.project

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.base.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to convert a list of ProjectWrappers into a list of full Project domain models.
 * It fetches details for each project using the ProjectRepository.
 */
class ConvertProjectWrappersToProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * Invokes the use case to convert project wrappers to full project details.
     * @param projectWrappers A list of ProjectWrapper objects.
     * @return A Flow emitting CustomResult containing a list of Project or an Exception.
     *         This flow will emit a list of successfully fetched projects. If any project fails to load,
     *         it will be omitted from the success list. 
     */
    operator fun invoke(projectWrappers: List<ProjectsWrapper>): Flow<CustomResult<List<Project>, Exception>> {
        if (projectWrappers.isEmpty()) {
            return flowOf(CustomResult.Success(emptyList()))
        }

        val projectDetailFlows: List<Flow<CustomResult<Project, Exception>>> = projectWrappers.map { wrapper ->
            projectRepository.getProjectDetailsStream(wrapper.projectId)
        }

        return combine(projectDetailFlows) { results ->
            val successfulProjects = mutableListOf<Project>()
            var firstError: Exception? = null

            results.forEach { result ->
                when (result) {
                    is CustomResult.Success -> successfulProjects.add(result.data)
                    is CustomResult.Failure -> {
                        if (firstError == null) firstError = result.error
                        // Optionally log other errors: println("Failed to load project: ${result.exception.message}")
                    }
                    is CustomResult.Loading -> {
                        // Handled by onStart or if all are loading
                    }
                    else -> {
                        Log.e("ConvertProjectWrappersToProjectsUseCase", "Unexpected result type: $result")
                    }
                }
            }

            if (results.all { it is CustomResult.Loading }) {
                CustomResult.Loading
            } else if (successfulProjects.isNotEmpty() || projectWrappers.isEmpty()) {
                CustomResult.Success(successfulProjects)
            } else {
                 CustomResult.Failure(firstError ?: Exception("Failed to load any project details and no specific error was captured."))
            }
        }.catch { e ->
            emit(CustomResult.Failure(Exception("Error combining project detail flows: ${e.message}", e)))
        }.onStart { emit(CustomResult.Loading) }
    }
}
