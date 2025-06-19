package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 내 역할(Role) 및 권한(Permission) 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface RoleRepository {
    fun getProjectRolesStream(projectId: String): Flow<CustomResult<List<Role>, Exception>>
    suspend fun createRole(
        projectId: String,
        name: String,
        isDefault: Boolean,
    ): CustomResult<String, Exception>

    suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        isDefault: Boolean?,
    ): CustomResult<Unit, Exception>

    suspend fun deleteRole(projectId: String, roleId: String): CustomResult<Unit, Exception>
    suspend fun getRoleDetails(projectId: String, roleId: String): CustomResult<Role, Exception>

    /**
     * Fetches the list of enabled permissions for a specific role.
     * Permissions are typically stored as a subcollection of the role.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role.
     * @return A [CustomResult] containing a list of enabled [RolePermission] enums on success, or an [Exception] on failure.
     */
    suspend fun getRolePermissions(projectId: String, roleId: String): CustomResult<List<RolePermission>, Exception>

    /**
     * Sets the enabled permissions for a specific role.
     * This will update the permissions subcollection for the role.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role.
     * @param permissions A list of [RolePermission] enums that should be enabled for the role.
     * @return A [CustomResult] indicating success ([Unit]) or failure ([Exception]).
     */
    suspend fun setRolePermissions(projectId: String, roleId: String, permissions: List<RolePermission>): CustomResult<Unit, Exception>
}
