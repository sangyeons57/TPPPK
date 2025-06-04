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
    // TODO: 필요한 Mapper 주입
) : ProjectsWrapperRepository {

    override fun getProjectsWrapperStream(userId: String): Flow<List<CustomResult<ProjectsWrapper, Exception>>> {
        return projectsWrapperRemoteDataSource.observeProjectsWrappers(userId).map {
            it.map { dto -> CustomResult.Success( dto.toDomain()) }
        }
    }

    /**
     * Adds a project wrapper to the specified user's collection by calling the remote data source.
     *
     * @param userId The ID of the user.
     * @param projectId The ID of the project.
     * @param projectsWrapper The project wrapper domain model to add.
     * @return A [CustomResult] indicating success or failure.
     */
    override suspend fun addProjectToUser(
        userId: String,
        projectId: String,
        projectsWrapper: ProjectsWrapper
    ): CustomResult<Unit, Exception> {
        // Map domain model to DTO
        val projectsWrapperDto = ProjectsWrapperDTO(
            projectName = projectsWrapper.projectName,
            projectImageUrl = projectsWrapper.projectImageUrl
            // Assuming DTO does not have an 'id' field as projectId is used as document ID in Firestore
        )
        return projectsWrapperRemoteDataSource.addProjectToUser(userId, projectId, projectsWrapperDto)
    }
}
