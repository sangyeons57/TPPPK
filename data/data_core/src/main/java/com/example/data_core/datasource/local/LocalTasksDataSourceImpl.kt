package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.dao.TasksDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Task
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.mapper.TaskEntityMapper
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
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: TaskEntityMapper
) : LocalTasksDataSource {

    companion object {
        private const val COLLECTION_NAME = "tasks"
    }

    override suspend fun getTaskById(taskId: String): Task? {
        val entity = tasksDao.getTaskById(taskId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getTasksByProject(projectId: String): List<Task> {
        val entities = tasksDao.getTasksByProject(projectId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByAssignee(assigneeId: String): List<Task> {
        val entities = tasksDao.getTasksByAssignee(assigneeId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByStatus(status: String): List<Task> {
        val entities = tasksDao.getTasksByStatus(status)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByChannel(channelId: String): List<Task> {
        val entities = tasksDao.getTasksByChannel(channelId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getAllTasks(): List<Task> {
        val entities = tasksDao.getAllTasks()
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun saveTask(task: Task) {
        val entity = mapper.toEntity(task)
        tasksDao.insertTask(entity)
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val entities = tasks.map { mapper.toEntity(it) }
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
        return entities.map { mapper.toDomain(it) }
    }

    override fun observeTasksByProject(projectId: String): Flow<List<Task>> {
        return tasksDao.observeTasksByProject(projectId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTaskById(taskId: String): Flow<Task?> {
        return tasksDao.observeTaskById(taskId).map { entity ->
            entity?.let { mapper.toDomain(it) }
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

    override fun observeTasksByType(taskType: TaskType): Flow<List<Task>> {
        return tasksDao.observeTasksByType(taskType.name).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return tasksDao.observeTasksByStatus(status.name).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTasksCheckedByUser(userId: String): Flow<List<Task>> {
        return tasksDao.observeTasksCheckedByUser(userId).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTasksByOrder(): Flow<List<Task>> {
        return tasksDao.observeTasksByOrder().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTasksByContent(content: String, limit: Int): Flow<List<Task>> {
        return tasksDao.searchTasksByContent(content, limit).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeAllTasks(): Flow<List<Task>> {
        return tasksDao.observeAllTasks().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTaskUpdatedAt(taskId: String): Flow<Long?> {
        return tasksDao.observeTaskById(taskId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeTasks(taskIds: List<String>): Flow<List<Task>> {
        return tasksDao.observeTasks(taskIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeTasksByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Task>> {
        return tasksDao.observeTasksByOrderRange(minOrder, maxOrder).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun getTasksByAssignee(assigneeId: String): List<Task> {
        return tasksDao.getTasksByAssignee(assigneeId).map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByStatus(status: TaskStatus): List<Task> {
        return tasksDao.getTasksByStatus(status.name).map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByChannel(channelId: String): List<Task> {
        return tasksDao.getTasksByChannel(channelId).map { mapper.toDomain(it) }
    }

    override suspend fun getAllTasks(limit: Int?): List<Task> {
        return tasksDao.getAllTasks().map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByIds(taskIds: List<String>): List<Task> {
        return tasksDao.getTasksByIds(taskIds).map { mapper.toDomain(it) }
    }

    override suspend fun getTasksByOrderRange(minOrder: Int, maxOrder: Int): List<Task> {
        return tasksDao.getTasksByOrderRange(minOrder, maxOrder).map { mapper.toDomain(it) }
    }

    override suspend fun getTasksCheckedAfter(timestamp: Instant): List<Task> {
        return tasksDao.getTasksCheckedAfter(timestamp).map { mapper.toDomain(it) }
    }

    override suspend fun getTaskCountByType(taskType: TaskType): Int {
        return tasksDao.getTaskCountByType(taskType.name)
    }

    override suspend fun getTaskCountByStatus(status: TaskStatus): Int {
        return tasksDao.getTaskCountByStatus(status.name)
    }

    override suspend fun getTotalTaskCount(): Int {
        return tasksDao.getTotalTaskCount()
    }

    override suspend fun getNextTaskOrder(): Int {
        return tasksDao.getNextTaskOrder()
    }
