package com.example.domain_usecase.usecase.project.authorization

import com.example.core_common.result.CustomResult
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import javax.inject.Inject

/**
 * Fetches the roles that a given user has within a specific project.
 */
interface GetUserRolesForProjectUseCase {
    /**
     * Returns the list of role IDs assigned to the user in the project.
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        userId: DocumentId
    ): CustomResult<List<DocumentId>, Exception>

    /**
     * Helper: Checks if the user has a specific role within the project.
     */
    suspend fun hasRole(
        projectId: DocumentId,
        userId: DocumentId,
        roleId: DocumentId
    ): CustomResult<Boolean, Exception>
}

class GetUserRolesForProjectUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository
) : GetUserRolesForProjectUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        userId: DocumentId
    ): CustomResult<List<DocumentId>, Exception> {
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        return when (val memberResult = memberRepository.findById(userId)) {
            is CustomResult.Success -> CustomResult.Success(memberResult.data.roleIds)
            is CustomResult.Failure -> CustomResult.Failure(memberResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(memberResult.progress)
        }
    }

    override suspend fun hasRole(
        projectId: DocumentId,
        userId: DocumentId,
        roleId: DocumentId
    ): CustomResult<Boolean, Exception> {
        return when (val res = invoke(projectId, userId)) {
            is CustomResult.Success -> CustomResult.Success(res.data.any { it.value == roleId.value })
            is CustomResult.Failure -> CustomResult.Failure(res.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(res.progress)
        }
    }
}

