package com.example.domain_usecase.usecase.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject

/**
 * 현재 사용자가 대상 멤버를 관리할 수 있는지 확인하는 UseCase
 *
 * 규칙:
 * 1. 오너는 자기 자신을 제외한 모든 멤버를 관리할 수 있음
 * 2. MEMBER_MANAGE 권한을 가진 멤버는 오너가 아닌 멤버만 관리할 수 있음
 * 3. 일반 멤버는 다른 멤버를 관리할 수 없음
 */
interface CanManageTargetMemberUseCase {
    /**
     * 현재 사용자가 대상 멤버를 관리할 수 있는지 확인합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 관리 대상 사용자 ID
     * @return 관리 가능 여부 (true: 관리 가능, false: 관리 불가)
     */
    suspend fun invoke(
        projectId: DocumentId,
        targetUserId: UserId
    ): CustomResult<Boolean, Exception>
}

/**
 * 대상 멤버 관리 권한 확인 UseCase 구현체
 */
class CanManageTargetMemberUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
    private val projectRoleRepository: ProjectRoleRepository
) : CanManageTargetMemberUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        targetUserId: UserId
    ): CustomResult<Boolean, Exception> {
        return try {
            // 1. 현재 사용자 정보 가져오기
            val currentUserResult = authRepository.getCurrentUserSession()
            if (currentUserResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("Failed to get current user session"))
            }

            val currentUserId = UserId.from(currentUserResult.data.userId.value)

            // 2. 자기 자신은 관리할 수 없음
            if (currentUserId == targetUserId) {
                return CustomResult.Success(false)
            }

            // 3. 프로젝트 정보 가져와서 오너 확인
            val projectResult = projectRepository.findById(projectId)
            if (projectResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("Failed to get project information"))
            }

            val project = projectResult.data
            val isCurrentUserOwner = project.ownerId.value == currentUserId.value
            val isTargetOwner = project.ownerId.value == targetUserId.value

            // 현재 사용자가 오너인 경우, 자기 자신을 제외한 모든 멤버를 관리할 수 있음
            if (isCurrentUserOwner) {
                return CustomResult.Success(true)
            }

            // 대상이 오너인 경우, 일반 멤버는 오너를 관리할 수 없음
            if (isTargetOwner) {
                return CustomResult.Success(false)
            }

            // 5. MEMBER_MANAGE 권한 확인 - HasProjectPermissionUseCase와 동일한 로직 사용
            return checkMemberManagePermission(projectId, currentUserId)

        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * MEMBER_MANAGE 권한 확인 helper method
     * HasProjectPermissionUseCase와 동일한 로직 사용
     */
    private suspend fun checkMemberManagePermission(
        projectId: DocumentId,
        userId: UserId
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
                is CustomResult.Success -> {
                    if (permsResult.data.contains(RolePermission.MEMBER_MANAGE)) {
                        return CustomResult.Success(true)
                    }
                }

                is CustomResult.Failure -> return CustomResult.Failure(permsResult.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(permsResult.progress)
            }
        }
        return CustomResult.Success(false)
    }
}