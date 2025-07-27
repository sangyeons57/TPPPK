package com.example.data.datasource.local

import com.example.domain.model.base.Task
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalTasksDataSource {

    suspend fun getTaskById(taskId: String): Task?
    suspend fun getTasksByProject(projectId: String): List<Task>
    suspend fun getTasksByAssignee(assigneeId: String): List<Task>
    suspend fun getTasksByStatus(status: String): List<Task>
    suspend fun getTasksByChannel(channelId: String): List<Task>
    suspend fun getAllTasks(): List<Task>

    suspend fun saveTask(task: Task)
    suspend fun saveTasks(tasks: List<Task>)
    suspend fun deleteTask(taskId: String)
    suspend fun deleteTasksByProject(projectId: String)

    suspend fun getTasksUpdatedAfter(timestamp: Instant): List<Task>
    fun observeTasksByProject(projectId: String): Flow<List<Task>>
    fun observeTaskById(taskId: String): Flow<Task?>

    suspend fun addToOutbox(taskId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<TaskOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)

    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    suspend fun taskExists(taskId: String): Boolean
    suspend fun getTaskCount(projectId: String): Int
    suspend fun clearAllTasks()
}

data class TaskOutboxOperation(
    val id: String,
    val taskId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)