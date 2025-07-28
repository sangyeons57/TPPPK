package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.TasksDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Task
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.domain.repository.local.LocalTaskRepository
import com.example.mapper.TaskEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Task Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalTaskRepositoryImpl @Inject constructor(
    private val tasksDao: TasksDao,
    private val outboxDao: OutboxDao,
    private val mapper: TaskEntityMapper
) : LocalTaskRepository {

    companion object {
        private const val TAG = "LocalTaskRepository"
        private const val COLLECTION_NAME = "tasks"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeTaskById(taskId: String): Flow<Task?> {
        Log.d(TAG, "observeTaskById: $taskId")
        return tasksDao.observeTaskById(taskId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeTasksByType(taskType: TaskType): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByType: $taskType")
        return tasksDao.observeTasksByType(taskType.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByStatus: $status")
        return tasksDao.observeTasksByStatus(status.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeCompletedTasks(): Flow<List<Task>> {
        Log.d(TAG, "observeCompletedTasks")
        return tasksDao.observeTasksByStatus(TaskStatus.COMPLETED.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeInProgressTasks(): Flow<List<Task>> {
        Log.d(TAG, "observeInProgressTasks")
        return tasksDao.observeTasksByStatus(TaskStatus.IN_PROGRESS.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observePendingTasks(): Flow<List<Task>> {
        Log.d(TAG, "observePendingTasks")
        return tasksDao.observeTasksByStatus(TaskStatus.PENDING.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTasksCheckedByUser(userId: String): Flow<List<Task>> {
        Log.d(TAG, "observeTasksCheckedByUser: $userId")
        return tasksDao.observeTasksCheckedByUser(userId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTasksByOrder(): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByOrder")
        return tasksDao.observeTasksByOrder().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTasksByContent(content: String, limit: Int): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByContent: content='$content', limit=$limit")
        return tasksDao.searchTasksByContent(content, limit).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeAllTasks(): Flow<List<Task>> {
        Log.d(TAG, "observeAllTasks")
        return tasksDao.observeAllTasks().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTaskUpdatedAt(taskId: String): Flow<Long?> {
        Log.d(TAG, "observeTaskUpdatedAt: $taskId")
        return tasksDao.observeTaskById(taskId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeTasks(taskIds: List<String>): Flow<List<Task>> {
        Log.d(TAG, "observeTasks: ${taskIds.size} tasks")
        return tasksDao.observeTasks(taskIds).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeTasksByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByOrderRange: $minOrder-$maxOrder")
        return tasksDao.observeTasksByOrderRange(minOrder, maxOrder).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    // === 단순 읽기 작업 ===

    override suspend fun getTaskById(taskId: String): Task? {
        Log.d(TAG, "getTaskById: $taskId")
        return try {
            tasksDao.getTaskById(taskId)?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTaskById failed", e)
            null
        }
    }

    override suspend fun getTasksByType(taskType: TaskType): List<Task> {
        Log.d(TAG, "getTasksByType: $taskType")
        return try {
            tasksDao.getTasksByType(taskType.name).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByType failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByStatus(status: TaskStatus): List<Task> {
        Log.d(TAG, "getTasksByStatus: $status")
        return try {
            tasksDao.getTasksByStatus(status.name).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getCompletedTasks(): List<Task> {
        Log.d(TAG, "getCompletedTasks")
        return tasksDao.getTasksByStatus(TaskStatus.COMPLETED.name).map { mapper.toDomain(it) }
    }

    override suspend fun getInProgressTasks(): List<Task> {
        Log.d(TAG, "getInProgressTasks")
        return tasksDao.getTasksByStatus(TaskStatus.IN_PROGRESS.name).map { mapper.toDomain(it) }
    }

    override suspend fun getPendingTasks(): List<Task> {
        Log.d(TAG, "getPendingTasks")
        return tasksDao.getTasksByStatus(TaskStatus.PENDING.name).map { mapper.toDomain(it) }
    }

    override suspend fun getTasksCheckedByUser(userId: String): List<Task> {
        Log.d(TAG, "getTasksCheckedByUser: $userId")
        return try {
            tasksDao.getTasksCheckedByUser(userId).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksCheckedByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByOrder(): List<Task> {
        Log.d(TAG, "getTasksByOrder")
        return try {
            tasksDao.getTasksByOrder().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByOrder failed", e)
            emptyList()
        }
    }

    override suspend fun searchTasksByContent(content: String, limit: Int): List<Task> {
        Log.d(TAG, "searchTasksByContent: content='$content', limit=$limit")
        return try {
            tasksDao.searchTasksByContent(content, limit).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "searchTasksByContent failed", e)
            emptyList()
        }
    }

    override suspend fun getAllTasks(limit: Int?): List<Task> {
        Log.d(TAG, "getAllTasks: limit=$limit")
        return try {
            tasksDao.getAllTasks().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllTasks failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByIds(taskIds: List<String>): List<Task> {
        Log.d(TAG, "getTasksByIds: ${taskIds.size} tasks")
        return try {
            tasksDao.getTasksByIds(taskIds).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByOrderRange(minOrder: Int, maxOrder: Int): List<Task> {
        Log.d(TAG, "getTasksByOrderRange: $minOrder-$maxOrder")
        return try {
            tasksDao.getTasksByOrderRange(minOrder, maxOrder).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByOrderRange failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksCheckedAfter(timestamp: Instant): List<Task> {
        Log.d(TAG, "getTasksCheckedAfter: $timestamp")
        return try {
            tasksDao.getTasksCheckedAfter(timestamp).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksCheckedAfter failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveTask(task: Task): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveTask: ${task.id}")

            // 1. Room DB에 저장
            tasksDao.insertTask(mapper.toEntity(task))

            // 2. Outbox에 동기화 작업 추가
            val operation = if (task.isNew) "CREATE" else "UPDATE"
            addToOutbox(
                taskId = task.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Task saved and added to outbox: ${task.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveTask failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveTasks(tasks: List<Task>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveTasks: ${tasks.size} tasks")

            if (tasks.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            tasksDao.insertTasks(tasks.map { mapper.toEntity(it) })

            Log.d(TAG, "Bulk tasks saved: ${tasks.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveTasks failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteTask: $taskId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            tasksDao.deleteTask(taskId)

            // 2. Outbox에 삭제 작업 추가
            addToOutbox(
                taskId = taskId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Task deleted and added to outbox: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteTask failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateTask(
        taskId: String,
        taskType: TaskType?,
        status: TaskStatus?,
        content: TaskContent?,
        order: TaskOrder?,
        checkedBy: UserId?,
        checkedAt: Instant?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateTask: taskId=$taskId")

            // 1. 현재 작업 조회
            val currentTask = tasksDao.getTaskById(taskId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 업데이트 적용
            var updatedTask = currentTask

            taskType?.let { updatedTask = updatedTask.updateTaskType(it) }
            status?.let { updatedTask = updatedTask.updateStatus(it) }
            content?.let { updatedTask = updatedTask.updateContent(it) }
            order?.let { updatedTask = updatedTask.updateOrder(it) }

            // 3. 저장 (Outbox 포함)
            saveTask(updatedTask)

            Log.d(TAG, "Task updated: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateTask failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateTaskType(
        taskId: String,
        newTaskType: TaskType
    ): CustomResult<Unit, Exception> {
        return updateTask(taskId, taskType = newTaskType)
    }

    override suspend fun updateTaskTypeWithCheck(
        taskId: String,
        newTaskType: TaskType,
        checkedBy: UserId?,
        checkedAt: Instant?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateTaskTypeWithCheck: taskId=$taskId, type=$newTaskType")

            // 1. 현재 작업 조회
            val currentTask = tasksDao.getTaskById(taskId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 타입과 체크 정보를 함께 업데이트
            val updatedTask = currentTask.updateTaskType(newTaskType, checkedBy, checkedAt)

            // 3. 저장 (Outbox 포함)
            saveTask(updatedTask)

            Log.d(TAG, "Task type updated with check info: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateTaskTypeWithCheck failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateTaskStatus(
        taskId: String,
        newStatus: TaskStatus
    ): CustomResult<Unit, Exception> {
        return updateTask(taskId, status = newStatus)
    }

    override suspend fun updateTaskContent(
        taskId: String,
        newContent: TaskContent
    ): CustomResult<Unit, Exception> {
        return updateTask(taskId, content = newContent)
    }

    override suspend fun updateTaskOrder(
        taskId: String,
        newOrder: TaskOrder
    ): CustomResult<Unit, Exception> {
        return updateTask(taskId, order = newOrder)
    }

    override suspend fun completeTask(taskId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "completeTask: $taskId")

            // 1. 현재 작업 조회
            val currentTask = tasksDao.getTaskById(taskId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 완료 처리
            val updatedTask = currentTask.complete()

            // 3. 저장 (Outbox 포함)
            saveTask(updatedTask)

            Log.d(TAG, "Task completed: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "completeTask failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun startTaskProgress(taskId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "startTaskProgress: $taskId")

            // 1. 현재 작업 조회
            val currentTask = tasksDao.getTaskById(taskId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 진행 중으로 변경
            val updatedTask = currentTask.startProgress()

            // 3. 저장 (Outbox 포함)
            saveTask(updatedTask)

            Log.d(TAG, "Task started progress: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "startTaskProgress failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun markTaskAsPending(taskId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "markTaskAsPending: $taskId")

            // 1. 현재 작업 조회
            val currentTask = tasksDao.getTaskById(taskId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 대기 중으로 변경
            val updatedTask = currentTask.markAsPending()

            // 3. 저장 (Outbox 포함)
            saveTask(updatedTask)

            Log.d(TAG, "Task marked as pending: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "markTaskAsPending failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun reorderTasks(taskOrderMap: Map<String, Int>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "reorderTasks: ${taskOrderMap.size} tasks")

            // 각 작업의 순서를 업데이트
            for ((taskId, newOrder) in taskOrderMap) {
                updateTaskOrder(taskId, TaskOrder(newOrder))
            }

            Log.d(TAG, "Tasks reordered successfully")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "reorderTasks failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun taskExists(taskId: String): Boolean {
        return try {
            tasksDao.taskExists(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "taskExists failed", e)
            false
        }
    }

    override suspend fun contentExists(content: TaskContent, excludeTaskId: String?): Boolean {
        return try {
            tasksDao.getAllTasks().any { task ->
                task.content == content.value && task.id != excludeTaskId
            }
        } catch (e: Exception) {
            Log.e(TAG, "contentExists failed", e)
            false
        }
    }

    override suspend fun getTaskCountByType(taskType: TaskType): Int {
        return try {
            tasksDao.getTaskCountByType(taskType.name)
        } catch (e: Exception) {
            Log.e(TAG, "getTaskCountByType failed", e)
            0
        }
    }

    override suspend fun getTaskCountByStatus(status: TaskStatus): Int {
        return try {
            tasksDao.getTaskCountByStatus(status.name)
        } catch (e: Exception) {
            Log.e(TAG, "getTaskCountByStatus failed", e)
            0
        }
    }

    override suspend fun getTotalTaskCount(): Int {
        return try {
            tasksDao.getTotalTaskCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalTaskCount failed", e)
            0
        }
    }

    override suspend fun getCompletedTaskCount(): Int {
        return getTaskCountByStatus(TaskStatus.COMPLETED)
    }

    override suspend fun getInProgressTaskCount(): Int {
        return getTaskCountByStatus(TaskStatus.IN_PROGRESS)
    }

    override suspend fun getPendingTaskCount(): Int {
        return getTaskCountByStatus(TaskStatus.PENDING)
    }

    override suspend fun getNextTaskOrder(): Int {
        return try {
            tasksDao.getNextTaskOrder()
        } catch (e: Exception) {
            Log.e(TAG, "getNextTaskOrder failed", e)
            1
        }
    }

    override suspend fun clearAllTasks(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllTasks")

            tasksDao.deleteAllTasks()

            Log.d(TAG, "All tasks cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllTasks failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getTasksUpdatedAfter(timestamp: Instant): List<Task> {
        return try {
            tasksDao.getTasksUpdatedAfter(timestamp).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getTasksUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        taskId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: taskId=$taskId, operation=$operation")

            val outboxEntity = OutboxEntity(
                id = UUID.randomUUID().toString(),
                collectionName = COLLECTION_NAME,
                documentId = taskId,
                operation = operation,
                payload = payload,
                localTimestamp = System.currentTimeMillis(),
                retries = 0
            )
            outboxDao.insertOutbox(outboxEntity)

            Log.d(TAG, "Added to outbox: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}