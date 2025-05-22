package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for retrieving a specific project member.
 */
interface GetProjectMemberUseCase {
    /**
     * Retrieves a specific member from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user (member) to retrieve.
     * @return A [Result] containing the [ProjectMember] if found, or null if not found, or an error.
     */
    suspend operator fun invoke(projectId: String, userId: String): Result<ProjectMember?>
}
/**
 * Implementation of [GetProjectMemberUseCase].
 */
class GetProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : GetProjectMemberUseCase {

    /**
     * Retrieves a specific member from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user (member) to retrieve.
     * @return A [Result] containing the [ProjectMember] if found, or null if not found, or an error.
     */
    override suspend fun invoke(projectId: String, userId: String): Result<ProjectMember?> {
        // The repository's getProjectMember already handles forceRefresh if needed by its own logic.
        // Here we directly call it.
        return projectMemberRepository.getProjectMember(projectId, userId)
    }
}
