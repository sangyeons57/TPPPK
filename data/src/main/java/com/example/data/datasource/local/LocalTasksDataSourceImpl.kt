package com.example.data.datasource.local

import com.example.data.dao.TasksDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.TasksMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTasksDataSourceImpl @Inject constructor(
    private val tasksDao: TasksDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalTasksDataSource {

    companion object {
        private const val COLLECTION_NAME = "tasks"
    }

    override suspend fun getTaskById(taskId: String): Task? {
        val entity = tasksDao.getTaskById(taskId) ?: return null
        return TasksMapper.toDomain(entity)
    }

    override suspend fun getTasksByProject(projectId: String): List<Task> {
        val entities = tasksDao.getTasksByProject(projectId)
        return TasksMapper.toDomainList(entities)
    }

    override suspend fun getTasksByAssignee(assigneeId: String): List<Task> {
        val entities = tasksDao.getTasksByAssignee(assigneeId)
        return TasksMapper.toDomainList(entities)
    }

    override suspend fun getTasksByStatus(status: String): List<Task> {
        val entities = tasksDao.getTasksByStatus(status)
        return TasksMapper.toDomainList(entities)
    }

    override suspend fun getTasksByChannel(channelId: String): List<Task> {
        val entities = tasksDao.getTasksByChannel(channelId)
        return TasksMapper.toDomainList(entities)
    }

    override suspend fun getAllTasks(): List<Task> {
        val entities = tasksDao.getAllTasks()
        return TasksMapper.toDomainList(entities)
    }

    override suspend fun saveTask(task: Task) {
        val entity = TasksMapper.toEntity(task)
        tasksDao.insertTask(entity)
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val entities = TasksMapper.toEntityList(tasks)
        tasksDao.insertTasks(entities)
    }

    override suspend fun deleteTask(taskId: String) {
        tasksDao.deleteTask(taskId)
    }

    override suspend fun deleteTasksByProject(projectId: String) {
        tasksDao.deleteTasksByProject(projectId)
    }

    override suspend fun getTasksUpdatedAfter(timestamp: Instant): List<Task> {
        val entities = tasksDao.getTasksUpdatedAfter(timestamp)
        return TasksMapper.toDomainList(entities)
    }

    override fun observeTasksByProject(projectId: String): Flow<List<Task>> {
        return tasksDao.observeTasksByProject(projectId).map { entities ->
            TasksMapper.toDomainList(entities)
        }
    }

    override fun observeTaskById(taskId: String): Flow<Task?> {
        return tasksDao.observeTaskById(taskId).map { entity ->
            entity?.let { TasksMapper.toDomain(it) }
        }
    }

    override suspend fun addToOutbox(taskId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = taskId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<TaskOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            TaskOutboxOperation(
                id = entity.id,
                taskId = entity.entityId,
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

    override suspend fun getLastSyncCursor(): Long? {
        return syncMetadataDao.getLastServerCursor(COLLECTION_NAME)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) {
            syncMetadataDao.initializeSyncMetadata(COLLECTION_NAME)
        }
        syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    override suspend fun taskExists(taskId: String): Boolean {
        return tasksDao.taskExists(taskId)
    }

    override suspend fun getTaskCount(projectId: String): Int {
        return tasksDao.getTaskCountByProject(projectId)
    }

    override suspend fun clearAllTasks() {
        tasksDao.deleteAllTasks()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}