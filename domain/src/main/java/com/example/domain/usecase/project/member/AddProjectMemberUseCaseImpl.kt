package com.example.domain.usecase.project.member

import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject
import kotlin.Result

/**
 * Implementation of [AddProjectMemberUseCase].
 */
class AddProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : AddProjectMemberUseCase {

    /**
     * Adds a user to a project with the specified initial roles.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user to add.
     * @param initialRoleIds The list of role IDs to assign to the member initially.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun invoke(
        projectId: String,
        userId: String,
        initialRoleIds: List<String>
    ): Result<Unit> {
        return projectMemberRepository.addMemberToProject(projectId, userId, initialRoleIds)
    }
}
