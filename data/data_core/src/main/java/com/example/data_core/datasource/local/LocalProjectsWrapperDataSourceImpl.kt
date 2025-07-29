package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.ProjectsWrapperDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.vo.project.ProjectName
import com.example.mapper.ProjectsWrapperEntityMapper
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
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: ProjectsWrapperEntityMapper
) : LocalProjectsWrapperDataSource {
    companion object {
        private const val COLLECTION_NAME = "projects_wrapper"
    }

    override suspend fun getWrapperById(wrapperId: String): ProjectsWrapper? =
        projectsWrapperDao.getWrapperById(wrapperId)?.let { mapper.toDomain(it) }

    override suspend fun getWrappersByUser(userId: String): List<ProjectsWrapper> =
        projectsWrapperDao.getWrappersByUser(userId).map { mapper.toDomain(it) }

    override suspend fun getWrapperByUserAndProject(
        userId: String,
        projectId: String
    ): ProjectsWrapper? = projectsWrapperDao.getWrapperByUserAndProject(userId, projectId)
        ?.let { mapper.toDomain(it) }

    override suspend fun getAllWrappers(): List<ProjectsWrapper> =
        projectsWrapperDao.getAllWrappers().map { mapper.toDomain(it) }

    override suspend fun saveWrapper(wrapper: ProjectsWrapper) =
        projectsWrapperDao.insertWrapper(mapper.toEntity(wrapper))

    override suspend fun saveWrappers(wrappers: List<ProjectsWrapper>) {
        if (wrappers.isNotEmpty()) projectsWrapperDao.insertWrappers(
            wrappers.map { mapper.toEntity(it) }
        )
    }

    override suspend fun deleteWrapper(wrapperId: String) =
        projectsWrapperDao.deleteWrapper(wrapperId)

    override suspend fun getWrappersUpdatedAfter(timestamp: Instant): List<ProjectsWrapper> =
        projectsWrapperDao.getWrappersUpdatedAfter(timestamp).map { mapper.toDomain(it) }

    override fun observeWrappersByUser(userId: String): Flow<List<ProjectsWrapper>> =
        projectsWrapperDao.observeWrappersByUser(userId)
            .map { it.map { entity -> mapper.toDomain(entity) } }

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
                it.documentId,
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

    override fun observeWrapperById(wrapperId: String): Flow<ProjectsWrapper?> {
        return projectsWrapperDao.observeWrapperById(wrapperId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeWrapperByUserAndProject(userId: String, projectId: String): Flow<ProjectsWrapper?> {
        return projectsWrapperDao.observeWrapperByUserAndProject(userId, projectId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeWrappersByProjectName(projectName: ProjectName): Flow<List<ProjectsWrapper>> {
        return projectsWrapperDao.observeWrappersByProjectName(projectName.value).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeWrappersByNameContaining(name: String, limit: Int): Flow<List<ProjectsWrapper>> {
        return projectsWrapperDao.searchWrappersByName(name, limit).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeWrappers(wrapperIds: List<String>): Flow<List<ProjectsWrapper>> {
        return projectsWrapperDao.observeWrappers(wrapperIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeWrapperUpdatedAt(wrapperId: String): Flow<Long?> {
        return projectsWrapperDao.observeWrapperById(wrapperId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllWrappers(): Flow<List<ProjectsWrapper>> {
        return projectsWrapperDao.observeAllWrappers().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeWrappersByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<ProjectsWrapper>> {
        return projectsWrapperDao.observeWrappersByOrderRange(minOrder, maxOrder).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun deleteDMWrappersByUser(userId: String) {
        projectsWrapperDao.deleteWrappersByUser(userId)
    }

    override suspend fun userHasProjectWrapper(userId: String, projectId: String): Boolean {
        return projectsWrapperDao.userHasProjectWrapper(userId, projectId)
    }

    override suspend fun projectNameExistsForUser(userId: String, projectName: ProjectName, excludeWrapperId: String?): Boolean {
        return projectsWrapperDao.projectNameExistsForUser(userId, projectName.value, excludeWrapperId)
    }

    override suspend fun getWrapperCountByUser(userId: String): Int {
        return projectsWrapperDao.getWrapperCountByUser(userId)
    }

    override suspend fun getTotalWrapperCount(): Int {
        return projectsWrapperDao.getTotalWrapperCount()
    }

    override suspend fun getNextWrapperOrder(userId: String): Int {
        return projectsWrapperDao.getNextWrapperOrder(userId)
    }

    override suspend fun clearUserWrappers(userId: String) {
        projectsWrapperDao.deleteWrappersByUser(userId)
    }
}