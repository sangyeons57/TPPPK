
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.RoleDTO
import kotlinx.coroutines.flow.Flow

interface RoleRemoteDataSource {

    /**
     * 특정 프로젝트의 모든 역할 목록을 실시간으로 관찰합니다.
     * @param projectId 역할을 가져올 프로젝트의 ID
     */
    fun observeRoles(projectId: String): Flow<CustomResult<List<RoleDTO>, Exception>>


    /**
     * 특정 프로젝트의 특정 역할 목록을 실시간으로 관찰합니다.
     * @param projectId 역할을 가져올 프로젝트의 ID
     */
    fun observeRole(projectId: String, roleId: String): Flow<CustomResult<RoleDTO, Exception>>

    /**
     * 프로젝트에 새로운 커스텀 역할을 추가합니다.
     * @param projectId 역할을 추가할 프로젝트의 ID
     * @param name 새로운 역할의 이름
     * @return 생성된 역할의 ID를 포함한 Result 객체
     */
    suspend fun addRole(projectId: String, roleDto: RoleDTO): CustomResult<String, Exception>

    /**
     * 프로젝트의 역할 이름을 수정합니다. (isDefault가 false인 역할만 가능)
     * @param projectId 대상 프로젝트의 ID
     * @param roleId 역할을 수정할 역할의 ID
     * @param newName 새로운 역할의 이름
     */
    suspend fun updateRole(projectId: String, roleId: String, newName: String): CustomResult<Unit, Exception>

    /**
     * 프로젝트에서 역할을 삭제합니다. (isDefault가 false인 역할만 가능)
     * @param projectId 대상 프로젝트의 ID
     * @param roleId 삭제할 역할의 ID
     */
    suspend fun deleteRole(projectId: String, roleId: String): CustomResult<Unit, Exception>

    /**
     * Fetches the names of all permissions granted to a specific role in a project.
     * These are typically the string representations of RolePermission enums.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role.
     * @return A [CustomResult] containing a list of permission name strings on success, or an [Exception] on failure.
     */
    suspend fun getRolePermissionNames(projectId: String, roleId: String): CustomResult<List<String>, Exception>

    /**
     * Sets (overwrites) the permissions for a specific role in a project.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role.
     * @param permissionNames A list of permission name strings (typically RolePermission enum names) to grant to the role.
     * @return A [CustomResult] indicating success (Unit) or an [Exception] on failure.
     */
    suspend fun setRolePermissions(projectId: String, roleId: String, permissionNames: List<String>): CustomResult<Unit, Exception>
}

