package com.example.domain.usecase.permission

import com.example.domain.model.ChannelType
import com.example.domain.model.RolePermission
import com.example.domain.model.ProjectMember
import com.example.domain.model.Role
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.ProjectMemberRepository
// import com.example.domain.repository.ProjectRoleRepository // ProjectRoleRepository는 이제 직접 사용하지 않음
import javax.inject.Inject
// Use Kotlin's standard Result type

/**
 * CheckChannelPermissionUseCase의 구현체입니다.
 *
 * @property channelRepository 채널 데이터 접근 리포지토리.
 * @property projectMemberRepository 프로젝트 멤버 데이터 접근 리포지토리.
 * @property projectRoleRepository 프로젝트 역할 데이터 접근 리포지토리. (이제 사용 안 함)
 */
class CheckChannelPermissionUseCaseImpl @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val projectMemberRepository: ProjectMemberRepository
    // private val projectRoleRepository: ProjectRoleRepository // 더 이상 필요 없음
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
            val memberResult = projectMemberRepository.getProjectMember(projectId, userId)
            val member = memberResult.getOrNull()
            
            // 멤버 정보가 없거나 (프로젝트에 속하지 않음) memberResult가 실패한 경우
            if (memberResult.isFailure || member == null) {
                 // memberResult.exceptionOrNull()이 null일 경우를 대비하여 기본 예외 제공
                return Result.failure(memberResult.exceptionOrNull() ?: IllegalStateException("User $userId not found in project $projectId or failed to fetch member data"))
            }
            
            // 사용자의 역할 목록 (ProjectMember.roles는 List<Role>)
            val userRoles = member.roles 
            
            for (role in userRoles) {
                if (role.permissions.contains(permission)) {
                    return Result.success(true)
                }
            }
            
            return Result.success(false) // No permission found after checking overrides and all roles
        } catch (e: Exception) {
            // Catch any unexpected exceptions during the process
            return Result.failure(e)
        }
    }
} 