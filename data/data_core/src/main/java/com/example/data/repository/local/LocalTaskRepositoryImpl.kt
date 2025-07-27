package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalTasksDataSource
import com.example.domain.model.base.Task
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.domain.repository.local.LocalTaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
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
    private val localTasksDataSource: LocalTasksDataSource
) : LocalTaskRepository {

    companion object {
        private const val TAG = "LocalTaskRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeTaskById(taskId: String): Flow<Task?> {
        Log.d(TAG, "observeTaskById: $taskId")
        return localTasksDataSource.observeTaskById(taskId)
    }

    override fun observeTasksByType(taskType: TaskType): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByType: $taskType")
        return localTasksDataSource.observeTasksByType(taskType)
    }

    override fun observeTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByStatus: $status")
        return localTasksDataSource.observeTasksByStatus(status)
    }

    override fun observeCompletedTasks(): Flow<List<Task>> {
        Log.d(TAG, "observeCompletedTasks")
        return observeTasksByStatus(TaskStatus.COMPLETED)
    }

    override fun observeInProgressTasks(): Flow<List<Task>> {
        Log.d(TAG, "observeInProgressTasks")
        return observeTasksByStatus(TaskStatus.IN_PROGRESS)
    }

    override fun observePendingTasks(): Flow<List<Task>> {
        Log.d(TAG, "observePendingTasks")
        return observeTasksByStatus(TaskStatus.PENDING)
    }

    override fun observeTasksCheckedByUser(userId: String): Flow<List<Task>> {
        Log.d(TAG, "observeTasksCheckedByUser: $userId")
        return localTasksDataSource.observeTasksCheckedByUser(userId)
    }

    override fun observeTasksByOrder(): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByOrder")
        return localTasksDataSource.observeTasksByOrder()
    }

    override fun observeTasksByContent(content: String, limit: Int): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByContent: content='$content', limit=$limit")
        return kotlinx.coroutines.flow.map(observeAllTasks()) { tasks ->
            tasks.filter { task ->
                task.content.value.contains(content, ignoreCase = true)
            }.take(limit)
        }
    }

    override fun observeAllTasks(): Flow<List<Task>> {
        Log.d(TAG, "observeAllTasks")
        return localTasksDataSource.observeAllTasks()
    }

    override fun observeTaskUpdatedAt(taskId: String): Flow<Long?> {
        Log.d(TAG, "observeTaskUpdatedAt: $taskId")
        return kotlinx.coroutines.flow.map(observeTaskById(taskId)) { task ->
            task?.updatedAt?.toEpochMilli()
        }
    }

    override fun observeTasks(taskIds: List<String>): Flow<List<Task>> {
        Log.d(TAG, "observeTasks: ${taskIds.size} tasks")
        return localTasksDataSource.observeTasks(taskIds)
    }

    override fun observeTasksByOrderRange(minOrder: Int, maxOrder: Int): Flow<List<Task>> {
        Log.d(TAG, "observeTasksByOrderRange: $minOrder-$maxOrder")
        return localTasksDataSource.observeTasksByOrderRange(minOrder, maxOrder)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getTaskById(taskId: String): Task? {
        Log.d(TAG, "getTaskById: $taskId")
        return try {
            localTasksDataSource.getTaskById(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "getTaskById failed", e)
            null
        }
    }

    override suspend fun getTasksByType(taskType: TaskType): List<Task> {
        Log.d(TAG, "getTasksByType: $taskType")
        return try {
            localTasksDataSource.getTasksByType(taskType)
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByType failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByStatus(status: TaskStatus): List<Task> {
        Log.d(TAG, "getTasksByStatus: $status")
        return try {
            localTasksDataSource.getTasksByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getCompletedTasks(): List<Task> {
        Log.d(TAG, "getCompletedTasks")
        return getTasksByStatus(TaskStatus.COMPLETED)
    }

    override suspend fun getInProgressTasks(): List<Task> {
        Log.d(TAG, "getInProgressTasks")
        return getTasksByStatus(TaskStatus.IN_PROGRESS)
    }

    override suspend fun getPendingTasks(): List<Task> {
        Log.d(TAG, "getPendingTasks")
        return getTasksByStatus(TaskStatus.PENDING)
    }

    override suspend fun getTasksCheckedByUser(userId: String): List<Task> {
        Log.d(TAG, "getTasksCheckedByUser: $userId")
        return try {
            localTasksDataSource.getTasksCheckedByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getTasksCheckedByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByOrder(): List<Task> {
        Log.d(TAG, "getTasksByOrder")
        return try {
            localTasksDataSource.getTasksByOrder()
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByOrder failed", e)
            emptyList()
        }
    }

    override suspend fun searchTasksByContent(content: String, limit: Int): List<Task> {
        Log.d(TAG, "searchTasksByContent: content='$content', limit=$limit")
        return try {
            getAllTasks().filter { task ->
                task.content.value.contains(content, ignoreCase = true)
            }.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchTasksByContent failed", e)
            emptyList()
        }
    }

    override suspend fun getAllTasks(limit: Int?): List<Task> {
        Log.d(TAG, "getAllTasks: limit=$limit")
        return try {
            localTasksDataSource.getAllTasks(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllTasks failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByIds(taskIds: List<String>): List<Task> {
        Log.d(TAG, "getTasksByIds: ${taskIds.size} tasks")
        return try {
            localTasksDataSource.getTasksByIds(taskIds)
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksByOrderRange(minOrder: Int, maxOrder: Int): List<Task> {
        Log.d(TAG, "getTasksByOrderRange: $minOrder-$maxOrder")
        return try {
            localTasksDataSource.getTasksByOrderRange(minOrder, maxOrder)
        } catch (e: Exception) {
            Log.e(TAG, "getTasksByOrderRange failed", e)
            emptyList()
        }
    }

    override suspend fun getTasksCheckedAfter(timestamp: Instant): List<Task> {
        Log.d(TAG, "getTasksCheckedAfter: $timestamp")
        return try {
            getAllTasks().filter { task ->
                task.checkedAt?.isAfter(timestamp) == true
            }
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
            localTasksDataSource.saveTask(task)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (task.isNew) "CREATE" else "UPDATE"
            localTasksDataSource.addToOutbox(
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
            localTasksDataSource.saveTasks(tasks)

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
            localTasksDataSource.deleteTask(taskId)

            // 2. Outbox에 삭제 작업 추가
            localTasksDataSource.addToOutbox(
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
            val currentTask = localTasksDataSource.getTaskById(taskId)
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 업데이트 적용
            var updatedTask = currentTask

            taskType?.let { updatedTask.updateTaskType(it) }
            status?.let { updatedTask.updateStatus(it) }
            content?.let { updatedTask.updateContent(it) }
            order?.let { updatedTask.updateOrder(it) }

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
            val currentTask = localTasksDataSource.getTaskById(taskId)
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 타입과 체크 정보를 함께 업데이트
            currentTask.updateTaskType(newTaskType, checkedBy, checkedAt)

            // 3. 저장 (Outbox 포함)
            saveTask(currentTask)

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
            val currentTask = localTasksDataSource.getTaskById(taskId)
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 완료 처리
            currentTask.complete()

            // 3. 저장 (Outbox 포함)
            saveTask(currentTask)

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
            val currentTask = localTasksDataSource.getTaskById(taskId)
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 진행 중으로 변경
            currentTask.startProgress()

            // 3. 저장 (Outbox 포함)
            saveTask(currentTask)

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
            val currentTask = localTasksDataSource.getTaskById(taskId)
                ?: return CustomResult.Failure(IllegalArgumentException("Task not found: $taskId"))

            // 2. 대기 중으로 변경
            currentTask.markAsPending()

            // 3. 저장 (Outbox 포함)
            saveTask(currentTask)

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
            localTasksDataSource.taskExists(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "taskExists failed", e)
            false
        }
    }

    override suspend fun contentExists(content: TaskContent, excludeTaskId: String?): Boolean {
        return try {
            getAllTasks().any { task ->
                task.content == content && task.id.value != excludeTaskId
            }
        } catch (e: Exception) {
            Log.e(TAG, "contentExists failed", e)
            false
        }
    }

    override suspend fun getTaskCountByType(taskType: TaskType): Int {
        return try {
            localTasksDataSource.getTaskCountByType(taskType)
        } catch (e: Exception) {
            Log.e(TAG, "getTaskCountByType failed", e)
            0
        }
    }

    override suspend fun getTaskCountByStatus(status: TaskStatus): Int {
        return try {
            localTasksDataSource.getTaskCountByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getTaskCountByStatus failed", e)
            0
        }
    }

    override suspend fun getTotalTaskCount(): Int {
        return try {
            localTasksDataSource.getTotalTaskCount()
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
            localTasksDataSource.getNextTaskOrder()
        } catch (e: Exception) {
            Log.e(TAG, "getNextTaskOrder failed", e)
            1
        }
    }

    override suspend fun clearAllTasks(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllTasks")

            localTasksDataSource.clearAllTasks()

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
            localTasksDataSource.getTasksUpdatedAfter(timestamp)
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

            localTasksDataSource.addToOutbox(taskId, operation, payload)

            Log.d(TAG, "Added to outbox: $taskId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}