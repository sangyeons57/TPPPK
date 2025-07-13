package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크를 삭제하는 유스케이스
 */
interface DeleteTaskUseCase {
    suspend operator fun invoke(taskId: String): CustomResult<Unit, Exception>
}

class DeleteTaskUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : DeleteTaskUseCase {

    override suspend operator fun invoke(taskId: String): CustomResult<Unit, Exception> {
        return when (val result = taskRepository.delete(DocumentId(taskId))) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}