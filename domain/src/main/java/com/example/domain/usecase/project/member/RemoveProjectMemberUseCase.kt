package com.example.domain.usecase.project.member

import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for removing a member from a project.
 */
interface RemoveProjectMemberUseCase {
    /**
     * Removes a user from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user to remove.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(projectId: String, userId: String): Result<Unit>
}

/**
 * Implementation of [RemoveProjectMemberUseCase].
 */
class RemoveProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : RemoveProjectMemberUseCase {

    /**
     * Removes a user from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user to remove.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun invoke(projectId: String, userId: String): Result<Unit> {
        return projectMemberRepository.removeMemberFromProject(projectId, userId)
    }
}
