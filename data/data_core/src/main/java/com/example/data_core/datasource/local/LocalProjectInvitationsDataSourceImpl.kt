package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.ProjectInvitationsDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.invite.InviteCode
import com.example.domain.model.vo.UserId
import com.example.mapper.ProjectInvitationEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProjectInvitationsDataSourceImpl @Inject constructor(
    private val projectInvitationsDao: ProjectInvitationsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: ProjectInvitationEntityMapper
) : LocalProjectInvitationsDataSource {
    companion object {
        private const val COLLECTION_NAME = "project_invitations"
    }

    override suspend fun getInvitationById(invitationId: String): ProjectInvitation? =
        projectInvitationsDao.getInvitationById(invitationId)
            ?.let { mapper.toDomain(it) }

    override suspend fun getInvitationsByProject(projectId: String): List<ProjectInvitation> =
        projectInvitationsDao.getInvitationsByProject(projectId).map { mapper.toDomain(it) }

    override suspend fun getInvitationsByUser(userId: String): List<ProjectInvitation> =
        projectInvitationsDao.getInvitationsByUser(userId).map { mapper.toDomain(it) }

    override suspend fun getAllInvitations(): List<ProjectInvitation> =
        projectInvitationsDao.getAllInvitations().map { mapper.toDomain(it) }

    override suspend fun saveInvitation(invitation: ProjectInvitation) =
        projectInvitationsDao.insertInvitation(mapper.toEntity(invitation))

    override suspend fun saveInvitations(invitations: List<ProjectInvitation>) {
        if (invitations.isNotEmpty()) projectInvitationsDao.insertInvitations(
            invitations.map { mapper.toEntity(it) }
        )
    }

    override suspend fun deleteInvitation(invitationId: String) =
        projectInvitationsDao.deleteInvitation(invitationId)

    override suspend fun getInvitationsUpdatedAfter(timestamp: Instant): List<ProjectInvitation> =
        projectInvitationsDao.getInvitationsUpdatedAfter(timestamp).map { mapper.toDomain(it) }

    override fun observeInvitationsByProject(projectId: String): Flow<List<ProjectInvitation>> =
        projectInvitationsDao.observeInvitationsByProject(projectId)
            .map { it.map { entity -> mapper.toDomain(entity) } }

    override suspend fun addToOutbox(invitationId: String, operation: String, payload: String?) =
        outboxDao.insertOperation(
            OutboxEntity(
                UUID.randomUUID().toString(),
                COLLECTION_NAME,
                invitationId,
                operation,
                payload,
                System.currentTimeMillis(),
                0
            )
        )

    override suspend fun getPendingOutboxOperations(): List<ProjectInvitationOutboxOperation> =
        outboxDao.getPendingOperationsByCollection(COLLECTION_NAME).map {
            ProjectInvitationOutboxOperation(
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

    override suspend fun clearAllInvitations() {
        projectInvitationsDao.deleteAllInvitations(); outboxDao.deleteOperationsByCollection(
            COLLECTION_NAME
        ); syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }

    override suspend fun getInvitationByCode(inviteCode: InviteCode): ProjectInvitation? {
        return projectInvitationsDao.getInvitationByCode(inviteCode.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun getInvitationsByStatus(status: InviteStatus): List<ProjectInvitation> {
        return projectInvitationsDao.getInvitationsByStatus(status.name).map { mapper.toDomain(it) }
    }

    override suspend fun getInvitationsByIds(invitationIds: List<String>): List<ProjectInvitation> {
        return projectInvitationsDao.getInvitationsByIds(invitationIds).map { mapper.toDomain(it) }
    }

    override suspend fun getActiveInvitations(): List<ProjectInvitation> {
        return projectInvitationsDao.getActiveInvitations().map { mapper.toDomain(it) }
    }

    override suspend fun getExpiredInvitations(): List<ProjectInvitation> {
        return projectInvitationsDao.getExpiredInvitations().map { mapper.toDomain(it) }
    }

    override suspend fun getInvitationsExpiringBefore(beforeTime: Instant): List<ProjectInvitation> {
        return projectInvitationsDao.getInvitationsExpiringBefore(beforeTime).map { mapper.toDomain(it) }
    }

    override fun observeInvitationById(invitationId: String): Flow<ProjectInvitation?> {
        return projectInvitationsDao.observeInvitationById(invitationId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeInvitationByCode(inviteCode: InviteCode): Flow<ProjectInvitation?> {
        return projectInvitationsDao.observeInvitationByCode(inviteCode.value).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeInvitationsByInviter(inviterId: UserId): Flow<List<ProjectInvitation>> {
        return projectInvitationsDao.observeInvitationsByInviter(inviterId.value).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeInvitationsByStatus(status: InviteStatus): Flow<List<ProjectInvitation>> {
        return projectInvitationsDao.observeInvitationsByStatus(status.name).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeInvitations(invitationIds: List<String>): Flow<List<ProjectInvitation>> {
        return projectInvitationsDao.observeInvitations(invitationIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeInvitationUpdatedAt(invitationId: String): Flow<Long?> {
        return projectInvitationsDao.observeInvitationById(invitationId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllInvitations(): Flow<List<ProjectInvitation>> {
        return projectInvitationsDao.observeAllInvitations().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeActiveInvitations(): Flow<List<ProjectInvitation>> {
        return projectInvitationsDao.observeActiveInvitations().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeExpiredInvitations(): Flow<List<ProjectInvitation>> {
        return projectInvitationsDao.observeExpiredInvitations().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun invitationExists(invitationId: String): Boolean {
        return projectInvitationsDao.invitationExists(invitationId)
    }

    override suspend fun inviteCodeExists(inviteCode: InviteCode): Boolean {
        return projectInvitationsDao.inviteCodeExists(inviteCode.value)
    }

    override suspend fun isInvitationActive(invitationId: String): Boolean {
        val invitation = projectInvitationsDao.getInvitationById(invitationId)?.let { mapper.toDomain(it) }
        return invitation?.isActive() ?: false
    }

    override suspend fun canInvitationBeUsed(invitationId: String): Boolean {
        val invitation = projectInvitationsDao.getInvitationById(invitationId)?.let { mapper.toDomain(it) }
        return invitation?.canBeUsed() ?: false
    }

    override suspend fun isInvitationExpired(invitationId: String): Boolean {
        val invitation = projectInvitationsDao.getInvitationById(invitationId)?.let { mapper.toDomain(it) }
        return invitation?.status == InviteStatus.EXPIRED ||
                (invitation?.expiresAt?.isBefore(Instant.now()) == true)
    }

    override suspend fun getTotalInvitationCount(): Int {
        return projectInvitationsDao.getTotalInvitationCount()
    }

    override suspend fun getInvitationCountByProject(projectId: String): Int {
        return projectInvitationsDao.getInvitationCountByProject(projectId)
    }

    override suspend fun getInvitationCountByStatus(status: InviteStatus): Int {
        return projectInvitationsDao.getInvitationCountByStatus(status.name)
    }

    override suspend fun getActiveInvitationCount(): Int {
        return projectInvitationsDao.getActiveInvitationCount()
    }
}