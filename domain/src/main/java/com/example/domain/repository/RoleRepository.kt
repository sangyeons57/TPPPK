package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 프로젝트 내 역할(Role) 및 권한(Permission) 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface RoleRepository {
    fun getProjectRolesStream(projectId: String): Flow<CustomResult<List<Role>, Exception>>
    suspend fun createRole(
        projectId: String,
        name: String,
        permissions: List<String>,
        color: String?,
        isDefault: Boolean,
        currentUserId: String
    ): CustomResult<Role, Exception>

    suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        permissions: List<String>?,
        color: String?,
        isDefault: Boolean?,
        currentUserId: String
    ): CustomResult<Unit, Exception>

    suspend fun deleteRole(projectId: String, roleId: String, currentUserId: String): CustomResult<Unit, Exception>
    suspend fun getRoleDetails(projectId: String, roleId: String): CustomResult<Role, Exception>
    suspend fun getAvailablePermissions(): CustomResult<List<String>, Exception>
}
