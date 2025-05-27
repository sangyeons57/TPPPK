package com.example.domain.usecase.permission

// Required new interface:
// interface ProjectChannelRepository {
//     fun getProjectChannelDetails(channelId: String): Result<ProjectChannelModel> // Or Channel
//     fun getChannelPermissionOverridesForUser(channelId: String, userId: String): Result<Map<RolePermission, Boolean>>
//     // ... other project channel methods
// }

// Required change for DmRepository:
// interface DmRepository {
//     // ... other methods
//     fun getDmChannelDetails(channelId: String): Result<DmChannelModel> // Or Channel
// }
// TODO: Prefer DmChannelModel and ProjectChannelModel over generic Channel model.

import com.example.domain.model.Channel // TODO: Replace with DmChannelModel/ProjectChannelModel as appropriate
import com.example.domain.model.RolePermission
import com.example.domain.repository.DmRepository
import com.example.domain.repository.ProjectChannelRepository
import com.example.domain.repository.ProjectMemberRepository
import javax.inject.Inject

/**
 * CheckChannelPermissionUseCase의 구현체입니다.
 *
 * @property dmRepository DM 채널 데이터 접근 리포지토리.
 * @property projectChannelRepository 프로젝트 채널 데이터 접근 리포지토리.
 * @property projectMemberRepository 프로젝트 멤버 데이터 접근 리포지토리.
 */
class CheckChannelPermissionUseCaseImpl @Inject constructor(
    private val dmRepository: DmRepository,
    private val projectChannelRepository: ProjectChannelRepository,
    private val projectMemberRepository: ProjectMemberRepository
) : CheckChannelPermissionUseCase {

    override suspend operator fun invoke(userId: String, channelId: String, permission: RolePermission): Result<Boolean> {
        try {
            // Attempt DM Channel First
            // TODO: Use DmChannelModel once available
            val dmChannelResult = dmRepository.getDmChannelDetails(channelId)
            if (dmChannelResult.isSuccess) {
                val dmChannel = dmChannelResult.getOrNull()
                if (dmChannel != null) {
                    // Assuming dmChannel is of type Channel (or DmChannelModel with similar structure)
                    // and has dmSpecificData.
                    val isParticipant = dmChannel.dmSpecificData?.participantIds?.contains(userId) == true
                    return when (permission) {
                        RolePermission.READ_MESSAGES,
                        RolePermission.SEND_MESSAGES -> Result.success(isParticipant)
                        else -> Result.success(false) // DMs have limited permissions
                    }
                }
            }
            // Log if DM channel fetch failed for reasons other than "not found", if necessary.
            // For now, we just proceed to project channel if dmChannel is null or fetch failed.

            // Attempt Project Channel If DM Fails or is not found
            // TODO: Use ProjectChannelModel once available
            val projectChannelResult = projectChannelRepository.getProjectChannelDetails(channelId)
            if (projectChannelResult.isSuccess) {
                val projectChannel = projectChannelResult.getOrNull()
                if (projectChannel != null) {
                    // Assuming projectChannel is of type Channel (or ProjectChannelModel with similar structure)
                    // and has projectSpecificData.
                    val projectId = projectChannel.projectSpecificData?.projectId
                        ?: return Result.failure(IllegalStateException("Project ID not found for project channel $channelId"))

                    // Check for user-specific overrides in the project channel
                    val overrideResult = projectChannelRepository.getChannelPermissionOverridesForUser(channelId, userId)
                    if (overrideResult.isSuccess) {
                        val overrides = overrideResult.getOrNull()
                        if (overrides != null && overrides.containsKey(permission)) {
                            return Result.success(overrides[permission] ?: false)
                        }
                    } else {
                        // Log or handle override fetch failure if critical, for now, we proceed
                        println("Warning: Could not fetch permission overrides for user $userId, channel $channelId: ${overrideResult.exceptionOrNull()?.message}")
                    }

                    // Check role-based permissions
                    val memberResult = projectMemberRepository.getProjectMember(projectId, userId)
                    val member = memberResult.getOrNull()
                    if (memberResult.isFailure || member == null) {
                        return Result.failure(memberResult.exceptionOrNull() ?: IllegalStateException("User $userId not found in project $projectId or failed to fetch member data"))
                    }

                    val userRoles = member.roles
                    for (role in userRoles) {
                        if (role.permissions.contains(permission)) {
                            return Result.success(true)
                        }
                    }
                    return Result.success(false) // No permission from roles
                }
            }

            // Handle Channel Not Found in either repository
            // Prioritize DM channel failure message if it was a specific error, else generic.
            val dmError = dmChannelResult.exceptionOrNull()
            val projectError = projectChannelResult.exceptionOrNull()

            if (dmError != null && projectError != null) { // Both failed for reasons other than not found (e.g. network)
                 return Result.failure(IllegalStateException("Failed to fetch channel $channelId from both DM and Project sources. DM Error: ${dmError.message}, Project Error: ${projectError.message}", projectError))
            }
            // If one failed with error and other was "not found" (null result without specific exception), it's still "not found".
            
            return Result.failure(IllegalStateException("Channel $channelId not found in any known repository"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}