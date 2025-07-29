package com.example.data_core.datasource.local

import com.example.domain.model.base.Permission
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalPermissionsDataSource {
    suspend fun getPermissionById(permissionId: String): Permission?
    suspend fun getPermissionsByRole(roleId: String): List<Permission>
    suspend fun getAllPermissions(): List<Permission>
    suspend fun savePermission(permission: Permission)
    suspend fun savePermissions(permissions: List<Permission>)
    suspend fun deletePermission(permissionId: String)
    suspend fun getPermissionsUpdatedAfter(timestamp: Instant): List<Permission>
    fun observePermissionsByRole(roleId: String): Flow<List<Permission>>
    suspend fun addToOutbox(permissionId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<PermissionOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)
    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)
    suspend fun clearAllPermissions()
}

data class PermissionOutboxOperation(
    val id: String,
    val permissionId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)