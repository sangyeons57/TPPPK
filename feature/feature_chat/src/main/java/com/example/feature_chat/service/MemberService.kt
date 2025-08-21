package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.feature_chat.model.ProjectMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 프로젝트 멤버 관리를 담당하는 Service
 * 프로젝트 멤버 정보 로딩 및 실시간 업데이트를 담당합니다.
 */
class MemberService(
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val projectId: String,
    private val userProfileService: UserProfileService
) {
    
    private val tag = "MemberService"
    
    /**
     * 프로젝트 멤버 목록을 로딩합니다.
     */
    suspend fun loadMembers(): List<ProjectMember> {
        Log.d(tag, "loadMembers: Loading members for project $projectId")
        
        return try {
            val memberUseCases = projectMemberUseCaseProvider.createForProject(DocumentId(projectId))

            // Use single-shot UseCase instead of Flow
            when (val result = memberUseCases.getProjectMembersUseCase()) {
                is CustomResult.Success -> {
                    val domainMembers = result.data
                    Log.d(tag, "loadMembers: Successfully loaded ${domainMembers.size} members")

                    // Convert domain members to UI models
                    val members = domainMembers.map { member ->
                        // Load user profile to get real display name
                        userProfileService.loadUserProfile(member.id.value)

                        val displayName = userProfileService.getUserDisplayName(member.id.value)
                        val profileUrl = userProfileService.getCachedProfileUrl(member.id.value)

                        // Get role name from the first role (simplified for now)
                        val roleId = member.roleIds.firstOrNull()?.value
                        val roleName = roleId // We'll enhance this when we get role names

                        ProjectMember(
                            userId = member.id.value,
                            displayName = displayName,
                            profileUrl = profileUrl,
                            roleId = roleId,
                            roleName = roleName
                        )
                    }

                    Log.d(tag, "loadMembers: Converted ${members.size} members to UI models")
                    members
                }

                is CustomResult.Failure -> {
                    Log.e(tag, "loadMembers: Failed to load project members", result.error)
                    emptyList()
                }

                else -> {
                    Log.d(tag, "loadMembers: Loading or other state")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "loadMembers: Exception while loading members", e)
            emptyList()
        }
    }
    
    /**
     * 프로젝트 멤버 목록을 실시간으로 관찰합니다.
     */
    fun observeMembers(): Flow<List<ProjectMember>> {
        Log.d(tag, "observeMembers: Starting to observe members for project $projectId")
        
        val memberUseCases = projectMemberUseCaseProvider.createForProject(DocumentId(projectId))
        
        return memberUseCases.observeProjectMembersUseCase().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val domainMembers = result.data
                    Log.d(tag, "observeMembers: Observed ${domainMembers.size} members")
                    
                    // Convert domain members to UI models
                    domainMembers.map { member ->
                        val displayName = userProfileService.getUserDisplayName(member.id.value)
                        val profileUrl = userProfileService.getCachedProfileUrl(member.id.value)
                        val roleId = member.roleIds.firstOrNull()?.value
                        val roleName = roleId
                        
                        ProjectMember(
                            userId = member.id.value,
                            displayName = displayName,
                            profileUrl = profileUrl,
                            roleId = roleId,
                            roleName = roleName
                        )
                    }
                }
                is CustomResult.Failure -> {
                    Log.e(tag, "observeMembers: Failed to observe project members", result.error)
                    emptyList()
                }
                is CustomResult.Loading -> {
                    Log.d(tag, "observeMembers: Loading project members...")
                    emptyList()
                }
                else -> {
                    Log.d(tag, "observeMembers: State: ${result::class.simpleName}")
                    emptyList()
                }
            }
        }
    }
    
    /**
     * 특정 멤버의 정보를 새로고침합니다.
     */
    suspend fun refreshMember(userId: String): ProjectMember? {
        Log.d(tag, "refreshMember: Refreshing member info for user $userId")
        
        return try {
            // Load fresh user profile
            userProfileService.loadUserProfile(userId)
            
            val displayName = userProfileService.getUserDisplayName(userId)
            val profileUrl = userProfileService.getCachedProfileUrl(userId)
            
            // TODO: Get actual role information from UseCase
            ProjectMember(
                userId = userId,
                displayName = displayName,
                profileUrl = profileUrl,
                roleId = null,
                roleName = null
            )
        } catch (e: Exception) {
            Log.e(tag, "refreshMember: Exception while refreshing member $userId", e)
            null
        }
    }
}
