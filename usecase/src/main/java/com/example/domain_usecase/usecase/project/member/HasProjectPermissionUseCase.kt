package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * 사용자가 프로젝트에서 특정 권한을 가지고 있는지 확인하는 UseCase
 */
interface HasProjectPermissionUseCase {
    suspend fun invoke(
        projectId: DocumentId,
        userId: UserId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception>
}

/**
 * 사용자의 프로젝트 권한 확인 UseCase 구현체
 */
class HasProjectPermissionUseCaseImpl @Inject constructor(
    private val memberRepository: MemberRepository,
    private val projectRoleRepository: ProjectRoleRepository,
) : HasProjectPermissionUseCase {
    override suspend fun invoke(
        projectId: DocumentId,
        userId: UserId,
        permission: RolePermission
    ): CustomResult<Boolean, Exception> {
        val memberResult = memberRepository.findById(DocumentId.from(userId.value))
        val roleIds = when (memberResult) {
            is CustomResult.Success -> memberResult.data.roleIds
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