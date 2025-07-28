package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.RolesDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Role
import com.example.domain.model.vo.Name
import com.example.mapper.RoleEntityMapper
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
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: RoleEntityMapper
) : LocalRolesDataSource {
    companion object {
        private const val COLLECTION_NAME = "roles"
    }

    override suspend fun getRoleById(roleId: String): Role? =
        rolesDao.getRoleById(roleId)?.let { mapper.toDomain(it) }

    override suspend fun getRolesByProject(projectId: String): List<Role> =
        rolesDao.getRolesByProject(projectId).map { mapper.toDomain(it) }

    override suspend fun getAllRoles(): List<Role> =
        rolesDao.getAllRoles().map { mapper.toDomain(it) }

    override suspend fun saveRole(role: Role) = rolesDao.insertRole(mapper.toEntity(role))
    override suspend fun saveRoles(roles: List<Role>) {
        if (roles.isNotEmpty()) rolesDao.insertRoles(roles.map { mapper.toEntity(it) })
    }

    override suspend fun deleteRole(roleId: String) = rolesDao.deleteRole(roleId)
    override suspend fun getRolesUpdatedAfter(timestamp: Instant): List<Role> =
        rolesDao.getRolesUpdatedAfter(timestamp).map { mapper.toDomain(it) }

    override fun observeRolesByProject(projectId: String): Flow<List<Role>> =
        rolesDao.observeRolesByProject(projectId).map { it.map { entity -> mapper.toDomain(entity) } }

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

    override fun observeRoleById(roleId: String): Flow<Role?> {
        return rolesDao.observeRoleById(roleId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeByName(name: Name): Flow<Role?> {
        return rolesDao.observeByName(name.value).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Role>> {
        return rolesDao.observeAllByName(name, limit).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeRoles(roleIds: List<String>): Flow<List<Role>> {
        return rolesDao.observeRoles(roleIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeRoleUpdatedAt(roleId: String): Flow<Long?> {
        return rolesDao.observeRoleById(roleId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllRoles(): Flow<List<Role>> {
        return rolesDao.observeAllRoles().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeDefaultRoles(): Flow<List<Role>> {
        return rolesDao.observeDefaultRoles().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeSystemRoles(): Flow<List<Role>> {
        return rolesDao.observeSystemRoles().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun getRoleByName(name: Name): Role? {
        return rolesDao.getRoleByName(name.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun searchRolesByName(name: String, limit: Int): List<Role> {
        return rolesDao.searchRolesByName(name, limit).map { mapper.toDomain(it) }
    }

    override suspend fun getRolesByIds(roleIds: List<String>): List<Role> {
        return rolesDao.getRolesByIds(roleIds).map { mapper.toDomain(it) }
    }

    override suspend fun getDefaultRoles(): List<Role> {
        return rolesDao.getDefaultRoles().map { mapper.toDomain(it) }
    }

    override suspend fun getSystemRoles(): List<Role> {
        return rolesDao.getSystemRoles().map { mapper.toDomain(it) }
    }

    override suspend fun getOwnerRole(): Role? {
        return rolesDao.getOwnerRole()?.let { mapper.toDomain(it) }
    }

    override suspend fun nameExists(name: Name, excludeRoleId: String?): Boolean {
        return rolesDao.nameExists(name.value, excludeRoleId)
    }

    override suspend fun isSystemRole(roleId: String): Boolean {
        return rolesDao.isSystemRole(roleId)
    }

    override suspend fun isDefaultRole(roleId: String): Boolean {
        return rolesDao.isDefaultRole(roleId)
    }

    override suspend fun getTotalRoleCount(): Int {
        return rolesDao.getTotalRoleCount()
    }

    override suspend fun getRoleCountByProject(projectId: String): Int {
        return rolesDao.getRoleCountByProject(projectId)
    }

    override suspend fun getDefaultRoleCount(): Int {
        return rolesDao.getDefaultRoleCount()
    }

    override suspend fun getSystemRoleCount(): Int {
        return rolesDao.getSystemRoleCount()
    }
}