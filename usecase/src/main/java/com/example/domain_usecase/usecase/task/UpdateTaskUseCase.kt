package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Task
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskContent
import com.example.domain.vo.task.TaskOrder
import com.example.domain.vo.task.TaskStatus
import com.example.domain.vo.task.TaskType
import com.example.domain_repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크를 업데이트하는 유스케이스
 */
interface UpdateTaskUseCase {
    suspend operator fun invoke(
        taskId: String,
        content: String? = null,
        taskType: TaskType? = null,
        status: TaskStatus? = null,
        order: TaskOrder? = null
    ): CustomResult<Unit, Exception>
}

class UpdateTaskUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : UpdateTaskUseCase {
    
    override suspend operator fun invoke(
        taskId: String,
        content: String?,
        taskType: TaskType?,
        status: TaskStatus?,
        order: TaskOrder?
    ): CustomResult<Unit, Exception> {
        val task = when (val result = taskRepository.findById(DocumentId(taskId))) {
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }

        // Apply changes, then reconstitute with refreshed updatedAt for sync ordering
        taskType?.let { task.updateTaskType(it) }
        status?.let { task.updateStatus(it) }
        order?.let { task.updateOrder(it) }
        content?.let { task.updateContent(TaskContent(it)) }

        val updated = Task.fromDataSource(
            id = task.id,
            channelId = task.channelId,
            taskType = task.taskType,
            status = task.status,
            content = task.content,
            order = task.order,
            checkedBy = task.checkedBy,
            checkedAt = task.checkedAt,
            createdAt = task.createdAt,
            updatedAt = DateTimeUtil.nowInstant()
        )

        taskRepository.addTask(updated)
        return CustomResult.Success(Unit)
    }
}
