package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectRoleUseCaseProvider
import com.example.feature_chat.model.ProjectRole
import kotlinx.coroutines.flow.first

/**
 * 프로젝트 역할 관리를 담당하는 Service
 * 프로젝트 역할 정보 로딩 및 멘션에 필요한 역할 데이터를 제공합니다.
 */
class RoleService(
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
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

            // Loading이 아닌 상태(Success 또는 Failure)까지 기다림
            Log.d(tag, "loadRoles: Starting to observe roles flow...")
            val result = roleUseCases.getProjectRolesUseCase(DocumentId(projectId))
                .first { result ->
                    Log.d(tag, "loadRoles: Received flow emission: ${result::class.simpleName}")
                    when (result) {
                        is CustomResult.Success -> {
                            Log.d(tag, "loadRoles: Success state received, proceeding...")
                            true
                        }

                        is CustomResult.Failure -> {
                            Log.d(tag, "loadRoles: Failure state received, proceeding...")
                            true
                        }

                        else -> {
                            Log.d(tag, "loadRoles: Loading state, waiting...")
                            false
                        }
                    }
                }

            when (result) {
                is CustomResult.Success -> {
                    val domainRoles = result.data
                    Log.d(
                        tag,
                        "loadRoles: Successfully loaded ${domainRoles.size} roles from UseCase"
                    )

                    // 로드된 도메인 역할들 로그 출력
                    domainRoles.forEach { role ->
                        Log.d(tag, "  - Domain Role: ${role.name.value} (id: ${role.id.value})")
                    }

                    // Convert domain roles to UI models and filter out everyone
                    val projectRoles = domainRoles
                        .filter { role ->
                            val isEveryone = role.id.value == "everyone"
                            if (isEveryone) {
                                Log.d(
                                    tag,
                                    "loadRoles: Filtering out 'everyone' role from domain data"
                                )
                            }
                            !isEveryone // everyone 역할 제외 (ViewModel에서 특수 멘션으로 처리)
                        }
                        .map { role ->
                            Log.d(tag, "loadRoles: Converting role ${role.name.value} to UI model")
                            ProjectRole(
                                roleId = role.id.value,
                                roleName = role.name.value,
                                memberCount = 0, // 초기값, updateRoleMemberCounts에서 업데이트됨
                                color = "#8B5CF6" // 일반 역할은 보라색
                            )
                        }

                    val roles = projectRoles

                    Log.d(tag, "loadRoles: Final filtered roles list (${roles.size} roles):")
                    roles.forEach { role ->
                        Log.d(
                            tag,
                            "  - UI Role: ${role.roleName} (id: ${role.roleId}, color: ${role.color})"
                        )
                    }

                    Log.d(
                        tag,
                        "loadRoles: Converted ${roles.size} roles to UI models (everyone filtered out)"
                    )
                    roles
                }

                is CustomResult.Failure -> {
                    Log.e(tag, "loadRoles: Failed to load project roles", result.error)
                    Log.e(tag, "loadRoles: Returning empty roles list instead of fallback everyone")
                    // 실패 시 빈 리스트 반환 (everyone은 ViewModel에서 특수 멘션으로 처리)
                    emptyList()
                }

                else -> {
                    Log.d(tag, "loadRoles: Loading or other state, returning empty list")
                    // 로딩 중이거나 기타 상태일 때도 빈 리스트 반환
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "loadRoles: Exception while loading roles", e)
            Log.e(tag, "loadRoles: Exception occurred, returning empty list")
            // 예외 발생 시 빈 리스트 반환
            emptyList()
        }
    }
    
    /**
     * 역할 멤버 수를 업데이트합니다.
     */
    suspend fun updateRoleMemberCounts(
        roles: List<ProjectRole>
    ): List<ProjectRole> {
        Log.d(tag, "updateRoleMemberCounts: Updating member counts for ${roles.size} roles")

        return try {
            val memberUseCases =
                projectMemberUseCaseProvider.createForProject(DocumentId(projectId))

            roles.map { role ->
                Log.d(tag, "updateRoleMemberCounts: Calculating count for role '${role.roleName}'")

                val memberCountResult =
                    memberUseCases.getRoleMemberCountUseCase(DocumentId(role.roleId))

                val memberCount = when (memberCountResult) {
                    is CustomResult.Success -> {
                        val count = memberCountResult.data
                        Log.d(
                            tag,
                            "updateRoleMemberCounts: Role '${role.roleName}' has $count members"
                        )
                        count
                    }

                    is CustomResult.Failure -> {
                        Log.e(
                            tag,
                            "updateRoleMemberCounts: Failed to get member count for role '${role.roleName}'",
                            memberCountResult.error
                        )
                        0
                    }

                    else -> {
                        Log.w(
                            tag,
                            "updateRoleMemberCounts: Unexpected result for role '${role.roleName}'"
                        )
                        0
                    }
                }

                role.copy(memberCount = memberCount)
            }
        } catch (e: Exception) {
            Log.e(tag, "updateRoleMemberCounts: Exception while updating member counts", e)
            // 예외 발생 시 원본 roles 반환
            roles
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
                    memberCount = 0, // Will be updated by caller
                    color = "#6B7280" // @everyone은 회색 계열 색상
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
