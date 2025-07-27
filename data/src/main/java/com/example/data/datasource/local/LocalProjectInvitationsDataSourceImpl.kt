package com.example.data.datasource.local

import com.example.data.dao.ProjectInvitationsDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.ProjectInvitationsMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.ProjectInvitation
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
    private val syncMetadataDao: SyncMetadataDao
) : LocalProjectInvitationsDataSource {
    companion object {
        private const val COLLECTION_NAME = "project_invitations"
    }

    override suspend fun getInvitationById(invitationId: String): ProjectInvitation? =
        projectInvitationsDao.getInvitationById(invitationId)
            ?.let { ProjectInvitationsMapper.toDomain(it) }

    override suspend fun getInvitationsByProject(projectId: String): List<ProjectInvitation> =
        ProjectInvitationsMapper.toDomainList(
            projectInvitationsDao.getInvitationsByProject(projectId)
        )

    override suspend fun getInvitationsByUser(userId: String): List<ProjectInvitation> =
        ProjectInvitationsMapper.toDomainList(projectInvitationsDao.getInvitationsByUser(userId))

    override suspend fun getAllInvitations(): List<ProjectInvitation> =
        ProjectInvitationsMapper.toDomainList(projectInvitationsDao.getAllInvitations())

    override suspend fun saveInvitation(invitation: ProjectInvitation) =
        projectInvitationsDao.insertInvitation(ProjectInvitationsMapper.toEntity(invitation))

    override suspend fun saveInvitations(invitations: List<ProjectInvitation>) {
        if (invitations.isNotEmpty()) projectInvitationsDao.insertInvitations(
            ProjectInvitationsMapper.toEntityList(invitations)
        )
    }

    override suspend fun deleteInvitation(invitationId: String) =
        projectInvitationsDao.deleteInvitation(invitationId)

    override suspend fun getInvitationsUpdatedAfter(timestamp: Instant): List<ProjectInvitation> =
        ProjectInvitationsMapper.toDomainList(
            projectInvitationsDao.getInvitationsUpdatedAfter(timestamp)
        )

    override fun observeInvitationsByProject(projectId: String): Flow<List<ProjectInvitation>> =
        projectInvitationsDao.observeInvitationsByProject(projectId)
            .map { ProjectInvitationsMapper.toDomainList(it) }

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
}