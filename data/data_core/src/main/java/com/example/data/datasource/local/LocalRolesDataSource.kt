package com.example.data.datasource.local

import com.example.domain.model.base.Role
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalRolesDataSource {
    suspend fun getRoleById(roleId: String): Role?
    suspend fun getRolesByProject(projectId: String): List<Role>
    suspend fun getAllRoles(): List<Role>
    suspend fun saveRole(role: Role)
    suspend fun saveRoles(roles: List<Role>)
    suspend fun deleteRole(roleId: String)
    suspend fun getRolesUpdatedAfter(timestamp: Instant): List<Role>
    fun observeRolesByProject(projectId: String): Flow<List<Role>>
    suspend fun addToOutbox(roleId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<RoleOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)
    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)
    suspend fun clearAllRoles()
}

data class RoleOutboxOperation(
    val id: String,
    val roleId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)