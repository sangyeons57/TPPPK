package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Task
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskStatus
import com.example.domain_repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크 상태를 업데이트하는 유스케이스
 */
interface UpdateTaskStatusUseCase {
    suspend operator fun invoke(taskId: String, status: TaskStatus): CustomResult<Unit, Exception>
}

class UpdateTaskStatusUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : UpdateTaskStatusUseCase {
    
    override suspend operator fun invoke(taskId: String, status: TaskStatus): CustomResult<Unit, Exception> {
        val task = when (val result = taskRepository.findById(DocumentId(taskId))) {
            is CustomResult.Success -> result.data as Task
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }

        task.updateStatus(status)

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

        taskRepository.updateTask(updated)
        return CustomResult.Success(Unit)
    }
}
