package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * 현재 사용자의 프로젝트 내 모든 권한을 가져오는 UseCase
 */
interface GetUserPermissionsForProjectUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<Set<RolePermission>, Exception>
}

class GetUserPermissionsForProjectUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val projectRoleRepository: ProjectRoleRepository,
) : GetUserPermissionsForProjectUseCase {
    override suspend fun invoke(projectId: DocumentId): CustomResult<Set<RolePermission>, Exception> {
        // Resolve current user id
        val session = authRepository.getCurrentUserSession()
        val userId = when (session) {
            is CustomResult.Success -> session.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(session.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(session.progress)
        }

        // Get member
        val memberRes = memberRepository.findById(DocumentId.from(userId))
        val member = when (memberRes) {
            is CustomResult.Success -> memberRes.data
            is CustomResult.Failure -> return CustomResult.Failure(memberRes.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(memberRes.progress)
        }

        // Collect permissions from all roles
        val permissions = mutableSetOf<RolePermission>()
        for (roleId in member.roleIds) {
            when (val permsResult =
                projectRoleRepository.getRolePermissions(projectId.value, roleId.value)) {
                is CustomResult.Success -> permissions.addAll(permsResult.data)
                is CustomResult.Failure -> return CustomResult.Failure(permsResult.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(permsResult.progress)
            }
        }

        return CustomResult.Success(permissions)
    }
}