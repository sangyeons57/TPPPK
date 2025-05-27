package com.example.domain._repository

import com.example.domain.model.Role
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 프로젝트 내 역할(Role) 및 권한(Permission) 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface RoleRepository {
    fun getProjectRolesStream(projectId: String): Flow<Result<List<Role>>>
    suspend fun createRole(
        projectId: String,
        name: String,
        permissions: List<String>,
        color: String?,
        isDefault: Boolean,
        currentUserId: String
    ): Result<Role>

    suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        permissions: List<String>?,
        color: String?,
        isDefault: Boolean?,
        currentUserId: String
    ): Result<Unit>

    suspend fun deleteRole(projectId: String, roleId: String, currentUserId: String): Result<Unit>
    suspend fun getRoleDetails(projectId: String, roleId: String): Result<Role>
    suspend fun getAvailablePermissions(): Result<List<String>>
}
