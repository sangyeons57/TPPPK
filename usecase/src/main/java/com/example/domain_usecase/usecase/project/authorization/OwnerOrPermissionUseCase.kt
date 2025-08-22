package com.example.domain_usecase.usecase.project.authorization

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * Checks OWNER first; if not owner, checks specific permission for current user.
 * Single responsibility via invoke.
 */
interface OwnerOrPermissionUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception>
}

class OwnerOrPermissionUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val projectRoleRepository: ProjectRoleRepository,
) : OwnerOrPermissionUseCase {
    override suspend fun invoke(
        projectId: DocumentId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception> {
        // Resolve current user id
        val session = authRepository.getCurrentUserSession()
        val userId = when (session) {
            is CustomResult.Success -> session.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(session.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(session.progress)
        }

        // OWNER check
        val memberRes = memberRepository.findById(DocumentId.from(userId))
        val member = when (memberRes) {
            is CustomResult.Success -> memberRes.data
            is CustomResult.Failure -> return CustomResult.Failure(memberRes.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(memberRes.progress)
        }
        if (member.roleIds.any { it.value == Role.OWNER }) return CustomResult.Success(true)

        // Permission check across roles
        for (roleId in member.roleIds) {
            when (val permsResult =
                projectRoleRepository.getRolePermissions(projectId.value, roleId.value)) {
                is CustomResult.Success -> if (permsResult.data.contains(permission)) return CustomResult.Success(
                    true
                )

                is CustomResult.Failure -> return CustomResult.Failure(permsResult.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(permsResult.progress)
            }
        }
        return CustomResult.Success(false)
    }
}

