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
        title: String? = null,
        description: String? = null,
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
        title: String?,
        description: String?,
        taskType: TaskType?,
        status: TaskStatus?,
        order: TaskOrder?
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
        status?.let { task.updateStatus(it) }
        order?.let { task.updateOrder(it) }
        
        // Update content if title or description is provided
        if (title != null || description != null) {
            val newTitle = title ?: task.content.value.split("\n").firstOrNull() ?: ""
            val newDescription = description ?: task.content.value.split("\n").drop(1).joinToString("\n")
            
            val content = if (newDescription.isNotBlank()) {
                "$newTitle\n$newDescription"
            } else {
                newTitle
            }
            
            task.updateContent(TaskContent(content))
        }

        return when (val result = taskRepository.save(task)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}