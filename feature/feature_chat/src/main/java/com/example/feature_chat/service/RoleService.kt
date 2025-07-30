package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain_usecase.provider.project.ProjectRoleUseCaseProvider
import com.example.feature_chat.model.ProjectRole

/**
 * 프로젝트 역할 관리를 담당하는 Service
 * 프로젝트 역할 정보 로딩 및 멘션에 필요한 역할 데이터를 제공합니다.
 */
class RoleService(
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val projectId: String
) {
    
    private val tag = "RoleService"
    
    /**
     * 프로젝트 역할 목록을 로딩합니다.
     */
    suspend fun loadRoles(): List<ProjectRole> {
        Log.d(tag, "loadRoles: Loading roles for project $projectId")
        
        return try {
            val roleUseCases = projectRoleUseCaseProvider.createForProject(DocumentId(projectId))
            
            // Collect the first emission from the Flow to get initial role list
            val rolesFlow = roleUseCases.getProjectRolesUseCase(DocumentId(projectId))
            
            // Collect first successful result
            var roles = emptyList<ProjectRole>()
            rolesFlow.collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val domainRoles = result.data
                        Log.d(tag, "loadRoles: Successfully loaded ${domainRoles.size} roles")
                        
                        // Convert domain roles to UI models
                        val projectRoles = domainRoles.map { role ->
                            ProjectRole(
                                roleId = role.id.value,
                                roleName = role.name.value,
                                memberCount = 0 // We'll calculate this from members
                            )
                        }
                        
                        // Add special @everyone role
                        roles = projectRoles + ProjectRole(
                            roleId = "everyone",
                            roleName = "everyone",
                            memberCount = 0 // Will be calculated based on total members
                        )
                        
                        Log.d(tag, "loadRoles: Converted ${roles.size} roles to UI models")
                        return@collect // Exit after first successful result
                    }
                    is CustomResult.Failure -> {
                        Log.e(tag, "loadRoles: Failed to load project roles", result.error)
                        // Return at least the @everyone role
                        roles = listOf(
                            ProjectRole(
                                roleId = "everyone",
                                roleName = "everyone",
                                memberCount = 0
                            )
                        )
                        return@collect // Exit on failure
                    }
                    is CustomResult.Loading -> {
                        Log.d(tag, "loadRoles: Loading project roles...")
                        // Continue collecting
                    }
                    else -> {
                        Log.d(tag, "loadRoles: State: ${result::class.simpleName}")
                        // Continue collecting for other states
                    }
                }
            }
            
            roles
        } catch (e: Exception) {
            Log.e(tag, "loadRoles: Exception while loading roles", e)
            // Return at least the @everyone role
            listOf(
                ProjectRole(
                    roleId = "everyone",
                    roleName = "everyone",
                    memberCount = 0
                )
            )
        }
    }
    
    /**
     * 역할 멤버 수를 업데이트합니다.
     */
    suspend fun updateRoleMemberCounts(
        roles: List<ProjectRole>,
        totalMemberCount: Int
    ): List<ProjectRole> {
        Log.d(tag, "updateRoleMemberCounts: Updating member counts for ${roles.size} roles")
        
        return roles.map { role ->
            val memberCount = when (role.roleId) {
                "everyone" -> totalMemberCount
                else -> {
                    // TODO: Calculate actual member count for this role
                    // This would require querying members by role
                    role.memberCount
                }
            }
            
            role.copy(memberCount = memberCount)
        }
    }
    
    /**
     * 특정 역할의 정보를 새로고침합니다.
     */
    suspend fun refreshRole(roleId: String): ProjectRole? {
        Log.d(tag, "refreshRole: Refreshing role info for role $roleId")
        
        return try {
            if (roleId == "everyone") {
                // Special case for @everyone role
                return ProjectRole(
                    roleId = "everyone",
                    roleName = "everyone",
                    memberCount = 0 // Will be updated by caller
                )
            }
            
            // TODO: Implement proper role refresh from UseCase
            null
        } catch (e: Exception) {
            Log.e(tag, "refreshRole: Exception while refreshing role $roleId", e)
            null
        }
    }
    
    /**
     * 역할 이름으로 역할을 검색합니다.
     */
    fun findRoleByName(roles: List<ProjectRole>, roleName: String): ProjectRole? {
        return roles.find { it.roleName.equals(roleName, ignoreCase = true) }
    }
    
    /**
     * 멘션 가능한 역할 목록을 반환합니다.
     */
    fun getMentionableRoles(roles: List<ProjectRole>): List<ProjectRole> {
        // For now, all roles are mentionable
        // In the future, we might add permission checks here
        return roles
    }
}
