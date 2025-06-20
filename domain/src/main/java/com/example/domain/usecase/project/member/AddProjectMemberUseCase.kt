package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.MemberRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for adding a member to a project.
 */
interface AddProjectMemberUseCase {
    /**
     * Adds a user to a project with the specified initial roles.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user to add.
     * @param initialRoleIds The list of role IDs to assign to the member initially.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(projectId: String, userId: String, initialRoleIds: List<String>): CustomResult<Unit, Exception>
}
/**
 * Implementation of [AddProjectMemberUseCase].
 */
class AddProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
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
    ): CustomResult<Unit, Exception> {
        return projectMemberRepository.addMemberToProject(projectId, userId, initialRoleIds)
    }
}
