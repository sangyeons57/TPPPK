package com.example.data.repository

import androidx.room.util.recursiveFetchLongSparseArray
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.ProjectsWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.data.model.remote.ProjectsWrapperDTO // Added for DTO mapping

class ProjectsWrapperRepositoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource
) : ProjectsWrapperRepository {

    override fun observeProjectsWrappers(userId: String): Flow<List<ProjectsWrapper>> {
        // 데이터소스는 Flow<List<String>> (projectId 목록)을 반환
        // 이를 Flow<List<ProjectsWrapper>> (각 ProjectsWrapper는 projectId만 가짐)으로 변환
        return projectsWrapperRemoteDataSource.observeProjectsWrappers(userId).map { projectIds ->
            projectIds.map { projectId -> ProjectsWrapper(projectId = projectId) }
        }
    }

    override suspend fun addProjectToUser(
        userId: String,
        projectId: String
    ): CustomResult<Unit, Exception> {
        // ProjectsWrapperDTO는 이제 projectId만 가짐
        val projectsWrapperDto = ProjectsWrapperDTO(projectId = projectId)
        return projectsWrapperRemoteDataSource.addProjectToUser(userId, projectId, projectsWrapperDto)
    }

    override suspend fun removeProjectFromUser(userId: String, projectId: String): CustomResult<Unit, Exception> {
        return projectsWrapperRemoteDataSource.removeProjectFromUser(userId, projectId)
    }
}
