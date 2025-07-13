package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.repository.base.TaskRepository
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

        return when (val result = taskRepository.save(task)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}