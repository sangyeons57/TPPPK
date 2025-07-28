package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.TaskStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface UpdateTaskStatusLocalUseCase {
    suspend operator fun invoke(taskId: DocumentId, status: TaskStatus): CustomResult<Unit, Exception>
}

class UpdateTaskStatusLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : UpdateTaskStatusLocalUseCase {

    override suspend operator fun invoke(taskId: DocumentId, status: TaskStatus): CustomResult<Unit, Exception> {
        return try {
            taskLocalRepository.updateTaskStatus(taskId, status)
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Error(e)
        }
    }
} 