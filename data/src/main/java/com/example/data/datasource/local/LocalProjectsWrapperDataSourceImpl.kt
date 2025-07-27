package com.example.data.datasource.local

import com.example.data.dao.ProjectsWrapperDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.ProjectsWrapperMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.ProjectsWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProjectsWrapperDataSourceImpl @Inject constructor(
    private val projectsWrapperDao: ProjectsWrapperDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalProjectsWrapperDataSource {
    companion object {
        private const val COLLECTION_NAME = "projects_wrapper"
    }

    override suspend fun getWrapperById(wrapperId: String): ProjectsWrapper? =
        projectsWrapperDao.getWrapperById(wrapperId)?.let { ProjectsWrapperMapper.toDomain(it) }

    override suspend fun getWrappersByUser(userId: String): List<ProjectsWrapper> =
        ProjectsWrapperMapper.toDomainList(projectsWrapperDao.getWrappersByUser(userId))

    override suspend fun getWrapperByUserAndProject(
        userId: String,
        projectId: String
    ): ProjectsWrapper? = projectsWrapperDao.getWrapperByUserAndProject(userId, projectId)
        ?.let { ProjectsWrapperMapper.toDomain(it) }

    override suspend fun getAllWrappers(): List<ProjectsWrapper> =
        ProjectsWrapperMapper.toDomainList(projectsWrapperDao.getAllWrappers())

    override suspend fun saveWrapper(wrapper: ProjectsWrapper) =
        projectsWrapperDao.insertWrapper(ProjectsWrapperMapper.toEntity(wrapper))

    override suspend fun saveWrappers(wrappers: List<ProjectsWrapper>) {
        if (wrappers.isNotEmpty()) projectsWrapperDao.insertWrappers(
            ProjectsWrapperMapper.toEntityList(
                wrappers
            )
        )
    }

    override suspend fun deleteWrapper(wrapperId: String) =
        projectsWrapperDao.deleteWrapper(wrapperId)

    override suspend fun getWrappersUpdatedAfter(timestamp: Instant): List<ProjectsWrapper> =
        ProjectsWrapperMapper.toDomainList(projectsWrapperDao.getWrappersUpdatedAfter(timestamp))

    override fun observeWrappersByUser(userId: String): Flow<List<ProjectsWrapper>> =
        projectsWrapperDao.observeWrappersByUser(userId)
            .map { ProjectsWrapperMapper.toDomainList(it) }

    override suspend fun addToOutbox(wrapperId: String, operation: String, payload: String?) =
        outboxDao.insertOperation(
            OutboxEntity(
                UUID.randomUUID().toString(),
                COLLECTION_NAME,
                wrapperId,
                operation,
                payload,
                System.currentTimeMillis(),
                0
            )
        )

    override suspend fun getPendingOutboxOperations(): List<ProjectsWrapperOutboxOperation> =
        outboxDao.getPendingOperationsByCollection(COLLECTION_NAME).map {
            ProjectsWrapperOutboxOperation(
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

    override suspend fun clearAllWrappers() {
        projectsWrapperDao.deleteAllWrappers(); outboxDao.deleteOperationsByCollection(
            COLLECTION_NAME
        ); syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}