package com.example.data.datasource.local

import com.example.data.dao.RolesDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.RolesMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRolesDataSourceImpl @Inject constructor(
    private val rolesDao: RolesDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalRolesDataSource {
    companion object {
        private const val COLLECTION_NAME = "roles"
    }

    override suspend fun getRoleById(roleId: String): Role? =
        rolesDao.getRoleById(roleId)?.let { RolesMapper.toDomain(it) }

    override suspend fun getRolesByProject(projectId: String): List<Role> =
        RolesMapper.toDomainList(rolesDao.getRolesByProject(projectId))

    override suspend fun getAllRoles(): List<Role> =
        RolesMapper.toDomainList(rolesDao.getAllRoles())

    override suspend fun saveRole(role: Role) = rolesDao.insertRole(RolesMapper.toEntity(role))
    override suspend fun saveRoles(roles: List<Role>) {
        if (roles.isNotEmpty()) rolesDao.insertRoles(RolesMapper.toEntityList(roles))
    }

    override suspend fun deleteRole(roleId: String) = rolesDao.deleteRole(roleId)
    override suspend fun getRolesUpdatedAfter(timestamp: Instant): List<Role> =
        RolesMapper.toDomainList(rolesDao.getRolesUpdatedAfter(timestamp))

    override fun observeRolesByProject(projectId: String): Flow<List<Role>> =
        rolesDao.observeRolesByProject(projectId).map { RolesMapper.toDomainList(it) }

    override suspend fun addToOutbox(roleId: String, operation: String, payload: String?) =
        outboxDao.insertOperation(
            OutboxEntity(
                UUID.randomUUID().toString(),
                COLLECTION_NAME,
                roleId,
                operation,
                payload,
                System.currentTimeMillis(),
                0
            )
        )

    override suspend fun getPendingOutboxOperations(): List<RoleOutboxOperation> =
        outboxDao.getPendingOperationsByCollection(COLLECTION_NAME).map {
            RoleOutboxOperation(
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

    override suspend fun clearAllRoles() {
        rolesDao.deleteAllRoles(); outboxDao.deleteOperationsByCollection(COLLECTION_NAME); syncMetadataDao.deleteSyncMetadata(
            COLLECTION_NAME
        )
    }
}