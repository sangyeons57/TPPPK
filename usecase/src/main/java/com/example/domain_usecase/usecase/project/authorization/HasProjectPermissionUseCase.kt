package com.example.domain_usecase.usecase.project.authorization

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * Checks if a given user has a specific permission in a project.
 * Single responsibility via invoke.
 */
interface HasProjectPermissionUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        userId: DocumentId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception>
}

class HasProjectPermissionUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository,
    private val projectRoleRepository: ProjectRoleRepository,
) : HasProjectPermissionUseCase {
    override suspend fun invoke(
        projectId: DocumentId,
        userId: DocumentId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception> {
        val memberResult = memberRepository.findById(userId)
        val roleIds = when (memberResult) {
            is CustomResult.Success -> (memberResult.data as Member).roleIds
            is CustomResult.Failure -> return CustomResult.Failure(memberResult.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(memberResult.progress)
        }

        for (roleId in roleIds) {
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

