package com.example.data.datasource.local

import com.example.data.dao.OutboxDao
import com.example.data.dao.ProjectsDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.mapper.ProjectsMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 프로젝트 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalProjectsDataSourceImpl @Inject constructor(
    private val projectsDao: ProjectsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalProjectsDataSource {

    companion object {
        private const val COLLECTION_NAME = "projects"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getProjectById(projectId: String): Project? {
        val entity = projectsDao.getProjectById(projectId) ?: return null
        return ProjectsMapper.toDomain(entity)
    }

    override suspend fun getAllProjects(): List<Project> {
        val entities = projectsDao.getAllProjects()
        return ProjectsMapper.toDomainList(entities)
    }

    override suspend fun getProjectsByOwner(ownerId: String): List<Project> {
        val entities = projectsDao.getProjectsByOwner(ownerId)
        return ProjectsMapper.toDomainList(entities)
    }

    override suspend fun getProjectsByStatus(status: String): List<Project> {
        val entities = projectsDao.getProjectsByStatus(status)
        return ProjectsMapper.toDomainList(entities)
    }

    override suspend fun searchProjectsByName(nameQuery: String): List<Project> {
        val entities = projectsDao.searchProjectsByName("%$nameQuery%")
        return ProjectsMapper.toDomainList(entities)
    }

    override suspend fun saveProject(project: Project) {
        val entity = ProjectsMapper.toEntity(project)
        projectsDao.insertProject(entity)
    }

    override suspend fun saveProjects(projects: List<Project>) {
        if (projects.isEmpty()) return

        val entities = ProjectsMapper.toEntityList(projects)
        projectsDao.insertProjects(entities)
    }

    override suspend fun deleteProject(projectId: String) {
        projectsDao.deleteProject(projectId)
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getProjectsUpdatedAfter(timestamp: Instant): List<Project> {
        val entities = projectsDao.getProjectsUpdatedAfter(timestamp)
        return ProjectsMapper.toDomainList(entities)
    }

    override fun observeProjectById(projectId: String): Flow<Project?> {
        return projectsDao.observeProjectById(projectId).map { entity ->
            entity?.let { ProjectsMapper.toDomain(it) }
        }
    }

    override fun observeAllProjects(): Flow<List<Project>> {
        return projectsDao.observeAllProjects().map { entities ->
            ProjectsMapper.toDomainList(entities)
        }
    }

    override fun observeProjectsByOwner(ownerId: String): Flow<List<Project>> {
        return projectsDao.observeProjectsByOwner(ownerId).map { entities ->
            ProjectsMapper.toDomainList(entities)
        }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(projectId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = projectId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<ProjectOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            ProjectOutboxOperation(
                id = entity.id,
                projectId = entity.entityId,
                operation = entity.operation,
                payload = entity.payload,
                localTimestamp = entity.localTimestamp,
                retries = entity.retries
            )
        }
    }

    override suspend fun markOutboxOperationComplete(operationId: String) {
        outboxDao.deleteOperation(operationId)
    }

    override suspend fun incrementOutboxRetries(operationId: String) {
        outboxDao.incrementRetries(operationId, System.currentTimeMillis())
    }

    // === 동기화 메타데이터 관리 ===

    override suspend fun getLastSyncCursor(): Long? {
        return syncMetadataDao.getLastServerCursor(COLLECTION_NAME)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        // 동기화 메타데이터가 없으면 초기화
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) {
            syncMetadataDao.initializeSyncMetadata(COLLECTION_NAME)
        }

        syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    // === 유틸리티 ===

    override suspend fun projectExists(projectId: String): Boolean {
        return projectsDao.projectExists(projectId)
    }

    override suspend fun getProjectCount(): Int {
        return projectsDao.getProjectCount()
    }

    override suspend fun getProjectCountByOwner(ownerId: String): Int {
        return projectsDao.getProjectCountByOwner(ownerId)
    }

    override suspend fun clearAllProjects() {
        projectsDao.deleteAllProjects()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}