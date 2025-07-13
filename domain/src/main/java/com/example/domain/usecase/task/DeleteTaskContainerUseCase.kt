package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.TaskContainerRepository
import javax.inject.Inject

/**
 * 태스크 컨테이너를 삭제하는 유스케이스
 */
interface DeleteTaskContainerUseCase {
    suspend operator fun invoke(containerId: String): CustomResult<Unit, Exception>
}

class DeleteTaskContainerUseCaseImpl @Inject constructor(
    private val taskContainerRepository: TaskContainerRepository
) : DeleteTaskContainerUseCase {

    override suspend operator fun invoke(containerId: String): CustomResult<Unit, Exception> {
        return when (val result = taskContainerRepository.delete(DocumentId(containerId))) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}