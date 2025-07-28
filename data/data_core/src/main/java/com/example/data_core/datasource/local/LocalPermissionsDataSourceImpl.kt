package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.PermissionsDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.mapper.PermissionEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPermissionsDataSourceImpl @Inject constructor(
    private val permissionsDao: PermissionsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: PermissionEntityMapper
) : LocalPermissionsDataSource {
    companion object {
        private const val COLLECTION_NAME = "permissions"
    }

    override suspend fun getPermissionById(permissionId: String): Permission? =
        permissionsDao.getPermissionById(permissionId)?.let { mapper.toDomain(it) }

    override suspend fun getPermissionsByRole(roleId: String): List<Permission> =
        permissionsDao.getPermissionsByRole(roleId).map { mapper.toDomain(it) }

    override suspend fun getAllPermissions(): List<Permission> =
        permissionsDao.getAllPermissions().map { mapper.toDomain(it) }

    override suspend fun savePermission(permission: Permission) =
        permissionsDao.insertPermission(mapper.toEntity(permission))

    override suspend fun savePermissions(permissions: List<Permission>) {
        if (permissions.isNotEmpty()) permissionsDao.insertPermissions(
            permissions.map { mapper.toEntity(it) }
        )
    }

    override suspend fun deletePermission(permissionId: String) =
        permissionsDao.deletePermission(permissionId)

    override suspend fun getPermissionsUpdatedAfter(timestamp: Instant): List<Permission> =
        permissionsDao.getPermissionsUpdatedAfter(timestamp).map { mapper.toDomain(it) }

    override fun observePermissionsByRole(roleId: String): Flow<List<Permission>> =
        permissionsDao.observePermissionsByRole(roleId).map { it.map { entity -> mapper.toDomain(entity) } }

    override suspend fun addToOutbox(permissionId: String, operation: String, payload: String?) =
        outboxDao.insertOperation(
            OutboxEntity(
                UUID.randomUUID().toString(),
                COLLECTION_NAME,
                permissionId,
                operation,
                payload,
                System.currentTimeMillis(),
                0
            )
        )

    override suspend fun getPendingOutboxOperations(): List<PermissionOutboxOperation> =
        outboxDao.getPendingOperationsByCollection(COLLECTION_NAME).map {
            PermissionOutboxOperation(
                it.id,
                it.entityId,
                it.operation,
                it.payload,
                it.localTimestamp,
                it.retries
            )
        }

    override suspend fun markOutboxOperationComplete(operationId: String) =
        outboxDao.deleteOperation(operationId)

    override suspend fun incrementOutboxRetries(operationId: String) =
        outboxDao.incrementRetries(operationId, System.currentTimeMillis())

    override suspend fun getLastSyncCursor(): Long? =
        syncMetadataDao.getLastServerCursor(COLLECTION_NAME)

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) syncMetadataDao.initializeSyncMetadata(
            COLLECTION_NAME
        ); syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    override suspend fun clearAllPermissions() {
        permissionsDao.deleteAllPermissions(); outboxDao.deleteOperationsByCollection(
            COLLECTION_NAME
        ); syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }

    override fun observePermissionById(permissionId: String): Flow<Permission?> {
        return permissionsDao.observePermissionById(permissionId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observePermissions(permissionIds: List<String>): Flow<List<Permission>> {
        return permissionsDao.observePermissions(permissionIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observePermissionUpdatedAt(permissionId: String): Flow<Long?> {
        return permissionsDao.observePermissionById(permissionId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllPermissions(): Flow<List<Permission>> {
        return permissionsDao.observeAllPermissions().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observePermissionsByType(rolePermission: RolePermission): Flow<List<Permission>> {
        return permissionsDao.observePermissionsByType(rolePermission.name).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun getPermissionsByIds(permissionIds: List<String>): List<Permission> {
        return permissionsDao.getPermissionsByIds(permissionIds).map { mapper.toDomain(it) }
    }

    override suspend fun getPermissionsByType(rolePermission: RolePermission): List<Permission> {
        return permissionsDao.getPermissionsByType(rolePermission.name).map { mapper.toDomain(it) }
    }

    override suspend fun getPermissionsByRoles(roleIds: List<String>): List<Permission> {
        return permissionsDao.getPermissionsByRoles(roleIds).map { mapper.toDomain(it) }
    }

    override suspend fun permissionExists(permissionId: String): Boolean {
        return permissionsDao.permissionExists(permissionId)
    }

    override suspend fun roleHasPermission(roleId: String, rolePermission: RolePermission): Boolean {
        return permissionsDao.roleHasPermission(roleId, rolePermission.name)
    }

    override suspend fun permissionTypeExists(rolePermission: RolePermission): Boolean {
        return permissionsDao.permissionTypeExists(rolePermission.name)
    }

    override suspend fun getTotalPermissionCount(): Int {
        return permissionsDao.getTotalPermissionCount()
    }

    override suspend fun getPermissionCountByRole(roleId: String): Int {
        return permissionsDao.getPermissionCountByRole(roleId)
    }

    override suspend fun getPermissionCountByType(rolePermission: RolePermission): Int {
        return permissionsDao.getPermissionCountByType(rolePermission.name)
    }
}