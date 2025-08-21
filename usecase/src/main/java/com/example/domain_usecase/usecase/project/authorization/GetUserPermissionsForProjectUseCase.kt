package com.example.domain_usecase.usecase.project.authorization

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * Aggregates a user's effective permissions within a project by flattening
 * permissions across all roles assigned to the member.
 */
interface GetUserPermissionsForProjectUseCase {
    /**
     * Returns the flattened permission set for the given user within a project.
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        userId: DocumentId
    ): CustomResult<Set<RolePermission>, Exception>

    /**
     * Helper: Checks if the user has a specific permission within the project.
     */
    suspend fun hasPermission(
        projectId: DocumentId,
        userId: DocumentId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception>
}

class GetUserPermissionsForProjectUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository,
    private val projectRoleRepository: ProjectRoleRepository,
) : GetUserPermissionsForProjectUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        userId: DocumentId
    ): CustomResult<Set<RolePermission>, Exception> {
        // Ensure repositories are scoped to the project's collections
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        // projectRoleRepository may manage permissions per role via explicit method below

        // Get user's role ids first
        val roleIds = when (val memberResult = memberRepository.findById(userId)) {
            is CustomResult.Success -> memberResult.data.roleIds
            is CustomResult.Failure -> return CustomResult.Failure(memberResult.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(memberResult.progress)
        }

        val aggregated = mutableSetOf<RolePermission>()
        for (roleId in roleIds) {
            when (val permsResult =
                projectRoleRepository.getRolePermissions(projectId.value, roleId.value)) {
                is CustomResult.Success -> aggregated.addAll(permsResult.data)
                is CustomResult.Failure -> return CustomResult.Failure(permsResult.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(permsResult.progress)
            }
        }
        return CustomResult.Success(aggregated)
    }

    override suspend fun hasPermission(
        projectId: DocumentId,
        userId: DocumentId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception> {
        return when (val res = invoke(projectId, userId)) {
            is CustomResult.Success -> CustomResult.Success(res.data.contains(permission))
            is CustomResult.Failure -> CustomResult.Failure(res.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(res.progress)
        }
    }
}

