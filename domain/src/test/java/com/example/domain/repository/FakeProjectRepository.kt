package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ProjectRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProjectRepository : ProjectRepository {

    private val projects = mutableMapOf<String, Project>()
    private val projectWrappers = mutableMapOf<String, ProjectsWrapper>()
    private var shouldThrowError = false

    override val factoryContext: DefaultRepositoryFactoryContext
        get() = TODO("Not yet implemented")

    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }

    fun addProject(project: Project) {
        projects[project.id.value] = project
    }

    fun addProjectWrapper(projectWrapper: ProjectsWrapper) {
        projectWrappers[projectWrapper.project.id.value] = projectWrapper
    }

    override suspend fun findById(
        id: DocumentId,
        source: Source
    ): CustomResult<AggregateRoot, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find by id failed"))
        }
        return projects[id.value]?.let {
            CustomResult.Success(it)
        } ?: CustomResult.Failure(Exception("Project not found"))
    }

    override suspend fun create(
        id: DocumentId,
        entity: AggregateRoot
    ): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Create failed"))
        }
        val project = entity as Project
        projects[id.value] = project
        return CustomResult.Success(id)
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Save failed"))
        }
        val project = entity as Project
        projects[project.id.value] = project
        return CustomResult.Success(project.id)
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Delete failed"))
        }
        return if (projects.remove(id.value) != null) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Project not found"))
        }
    }

    override suspend fun findAll(source: Source): CustomResult<List<AggregateRoot>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find all failed"))
        }
        return CustomResult.Success(projects.values.toList())
    }

    override fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe failed")))
        }
        return flowOf(
            projects[id.value]?.let { CustomResult.Success(it) }
                ?: CustomResult.Failure(Exception("Project not found"))
        )
    }

    override fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe all failed")))
        }
        return flowOf(CustomResult.Success(projects.values.toList()))
    }

    override suspend fun getUserParticipatingProjects(userId: UserId): CustomResult<List<ProjectsWrapper>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Get user projects failed"))
        }
        val userProjects = projectWrappers.values.filter { wrapper ->
            wrapper.project.members.any { it.userId == userId }
        }
        return CustomResult.Success(userProjects)
    }

    override fun getUserParticipatingProjectsStream(userId: UserId): Flow<CustomResult<List<ProjectsWrapper>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Get user projects stream failed")))
        }
        val userProjects = projectWrappers.values.filter { wrapper ->
            wrapper.project.members.any { it.userId == userId }
        }
        return flowOf(CustomResult.Success(userProjects))
    }

    override suspend fun deleteProject(
        projectId: DocumentId,
        userId: UserId
    ): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Delete project failed"))
        }
        val project = projects[projectId.value]
        return if (project != null && project.ownerId == userId) {
            projects.remove(projectId.value)
            projectWrappers.remove(projectId.value)
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Project not found or no permission"))
        }
    }

    override suspend fun joinProject(
        projectId: DocumentId,
        userId: UserId,
        inviteCode: String
    ): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Join project failed"))
        }
        val project = projects[projectId.value]
        return if (project != null) {
            // Simulate successful join
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Project not found"))
        }
    }

    override suspend fun leaveProject(
        projectId: DocumentId,
        userId: UserId
    ): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Leave project failed"))
        }
        val project = projects[projectId.value]
        return if (project != null) {
            // Simulate successful leave
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Project not found"))
        }
    }
}