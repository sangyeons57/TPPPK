package com.example.domain.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
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
    suspend operator fun invoke(projectId: String, userId: String): Flow<CustomResult<Member, Exception>>
}
/**
 * Implementation of [GetProjectMemberUseCase].
 */
class GetProjectMemberUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : GetProjectMemberUseCase {

    /**
     * Retrieves a specific member from a project.
     *
     * @param projectId The ID of the project.
     * @param userId The ID of the user (member) to retrieve.
     * @return A [Result] containing the [ProjectMember] if found, or null if not found, or an error.
     */
    override suspend fun invoke(projectId: String, userId: String): Flow<CustomResult<Member, Exception>> {
        // The repository's getProjectMember already handles forceRefresh if needed by its own logic.
        // Here we directly call it.
        return projectMemberRepository.getProjectMemberStream(projectId, userId)
    }
}
