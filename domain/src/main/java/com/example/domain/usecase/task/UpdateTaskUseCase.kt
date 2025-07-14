package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.domain.repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크를 업데이트하는 유스케이스
 */
interface UpdateTaskUseCase {
    suspend operator fun invoke(
        taskId: String,
        taskType: TaskType? = null,
        content: String? = null,
        order: Int? = null,
        status: TaskStatus? = null
    ): CustomResult<Unit, Exception>
}

class UpdateTaskUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : UpdateTaskUseCase {
    
    override suspend operator fun invoke(
        taskId: String,
        taskType: TaskType?,
        content: String?,
        order: Int?,
        status: TaskStatus?
    ): CustomResult<Unit, Exception> {
        val task = when (val result = taskRepository.findById(DocumentId(taskId))) {
            is CustomResult.Success -> result.data as Task
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }

        // Update fields if provided
        taskType?.let { task.updateTaskType(it) }
        content?.let { task.updateContent(TaskContent(it)) }
        order?.let { task.updateOrder(TaskOrder(it)) }
        status?.let { task.updateStatus(it) }

        return when (val result = taskRepository.save(task)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}