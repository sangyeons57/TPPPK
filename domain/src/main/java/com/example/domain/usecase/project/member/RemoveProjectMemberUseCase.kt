package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.repository.MemberRepository
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
    suspend operator fun invoke(projectId: String, userId: String): CustomResult<Unit, Exception>
}

/**
 * Implementation of [RemoveProjectMemberUseCase].
 */
class RemoveProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : RemoveProjectMemberUseCase {

    /**
     * Removes a user from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user to remove.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun invoke(projectId: String, userId: String): CustomResult<Unit, Exception> {
        return projectMemberRepository.removeProjectMember(projectId, userId)
    }
}
