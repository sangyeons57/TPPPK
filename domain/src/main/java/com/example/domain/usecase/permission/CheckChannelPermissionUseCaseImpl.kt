package com.example.domain.usecase.permission

import com.example.domain.model.ChannelType
import com.example.domain.model.RolePermission
import com.example.domain.model.ProjectMember // Correct model
import com.example.domain.model.Role // Correct model
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.ProjectMemberRepository // Correct repository for members
import com.example.domain.repository.ProjectRoleRepository // Assumed: domain/repository/ProjectRoleRepository.kt
import javax.inject.Inject
// Use Kotlin's standard Result type

/**
 * CheckChannelPermissionUseCase의 구현체입니다.
 *
 * @property channelRepository 채널 데이터 접근 리포지토리.
 * @property projectMemberRepository 프로젝트 멤버 데이터 접근 리포지토리.
 * @property projectRoleRepository 프로젝트 역할 데이터 접근 리포지토리.
 */
class CheckChannelPermissionUseCaseImpl @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val projectRoleRepository: ProjectRoleRepository
) : CheckChannelPermissionUseCase {

    /**
     * 지정된 사용자가 특정 채널에서 요구되는 권한을 가지고 있는지 비동기적으로 확인합니다.
     *
     * @param userId 권한을 확인할 사용자의 ID.
     * @param channelId 권한을 확인할 채널의 ID.
     * @param permission 확인할 [RolePermission].
     * @return 성공 시 사용자의 권한 보유 여부(Boolean)를 담은 [Result]. 실패 시 에러 정보를 포함한 [Result].
     */ // Use kotlin.Result
    override suspend operator fun invoke(userId: String, channelId: String, permission: RolePermission): Result<Boolean> {
        try {
            val channelResult = channelRepository.getChannel(channelId)
            val channel = channelResult.getOrNull() 
                ?: return Result.failure(channelResult.exceptionOrNull() ?: IllegalStateException("Channel $channelId not found or failed to fetch"))

            // 1. DM 채널의 경우 참가자 여부로 기본 권한 판단
            if (channel.type == ChannelType.DM) {
                val isParticipant = channel.dmSpecificData?.participantIds?.contains(userId) == true
                return when (permission) {
                    RolePermission.READ_MESSAGES,
                    RolePermission.SEND_MESSAGES -> Result.success(isParticipant) // Only if participant
                    // DM 채널에서 다른 권한은 기본적으로 false (DM은 오버라이드나 역할 없음)
                    else -> Result.success(false)
                }
            }

            // 2. 프로젝트/카테고리 채널의 경우
            val projectId = channel.projectSpecificData?.projectId
                ?: return Result.failure(IllegalStateException("Project ID not found for non-DM channel $channelId"))

            // 2a. 채널별 권한 재정의 확인 (user-specific overrides)
            val overrideResult = channelRepository.getChannelPermissionOverridesForUser(channelId, userId)
            if (overrideResult.isSuccess) {
                val overrides = overrideResult.getOrNull() // Map<RolePermission, Boolean>?
                if (overrides != null && overrides.containsKey(permission)) {
                    return Result.success(overrides[permission] ?: false) // Return override if exists
                }
            } else {
                 println("Warning: Could not fetch permission overrides for user $userId, channel $channelId: ${overrideResult.exceptionOrNull()?.message}")
                 // Optionally, if override fetch failure is critical:
                 // return Result.failure(overrideResult.exceptionOrNull() ?: IllegalStateException("Failed to fetch permission overrides"))
            }

            // 2b. 사용자 역할 기반 권한 확인
            // Use ProjectMemberRepository.getProjectMember which returns Result<ProjectMember?>
            val memberResult = projectMemberRepository.getProjectMember(projectId, userId)
            val member = memberResult.getOrNull()
                ?: return Result.failure(memberResult.exceptionOrNull() ?: IllegalStateException("User $userId member data fetch failed for project $projectId"))
            
            // if member is null (user not found in project), no role-based permissions.
            if (member == null) return Result.success(false)

            // Use ProjectMember model's roleIds: List<String> (non-nullable based on definition)
            val userRoleIds = member.roleIds
            if (userRoleIds.isEmpty()) {
                return Result.success(false) // No roles assigned, no permission by default
            }

            // Use ProjectRoleRepository.getRoles which returns Result<List<Role>>
            val allRolesResult = projectRoleRepository.getRoles(projectId)
            val allProjectRoles = allRolesResult.getOrNull() 
                ?: return Result.failure(allRolesResult.exceptionOrNull() ?: IllegalStateException("Failed to fetch roles for project $projectId"))

            // Filter the project roles to only those the user has
            val userRoles = allProjectRoles.filter { it.id in userRoleIds }
            
            // Iterate through the user's roles
            for (role in userRoles) {
                 // Use Role model's permissions: Map<RolePermission, Boolean>
                if (role.permissions[permission] == true) { // Check using RolePermission enum key
                    return Result.success(true) // Permission granted by at least one role
                }
            }
            
            return Result.success(false) // No permission found after checking overrides and all roles
        } catch (e: Exception) {
            // Catch any unexpected exceptions during the process
            return Result.failure(e)
        }
    }
} 