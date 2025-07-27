package com.example.data.datasource.local

import com.example.data.dao.OutboxDao
import com.example.data.dao.PermissionsDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.mapper.PermissionsMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Permission
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
    private val syncMetadataDao: SyncMetadataDao
) : LocalPermissionsDataSource {
    companion object {
        private const val COLLECTION_NAME = "permissions"
    }

    override suspend fun getPermissionById(permissionId: String): Permission? =
        permissionsDao.getPermissionById(permissionId)?.let { PermissionsMapper.toDomain(it) }

    override suspend fun getPermissionsByRole(roleId: String): List<Permission> =
        PermissionsMapper.toDomainList(permissionsDao.getPermissionsByRole(roleId))

    override suspend fun getAllPermissions(): List<Permission> =
        PermissionsMapper.toDomainList(permissionsDao.getAllPermissions())

    override suspend fun savePermission(permission: Permission) =
        permissionsDao.insertPermission(PermissionsMapper.toEntity(permission))

    override suspend fun savePermissions(permissions: List<Permission>) {
        if (permissions.isNotEmpty()) permissionsDao.insertPermissions(
            PermissionsMapper.toEntityList(
                permissions
            )
        )
    }

    override suspend fun deletePermission(permissionId: String) =
        permissionsDao.deletePermission(permissionId)

    override suspend fun getPermissionsUpdatedAfter(timestamp: Instant): List<Permission> =
        PermissionsMapper.toDomainList(permissionsDao.getPermissionsUpdatedAfter(timestamp))

    override fun observePermissionsByRole(roleId: String): Flow<List<Permission>> =
        permissionsDao.observePermissionsByRole(roleId).map { PermissionsMapper.toDomainList(it) }

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
}