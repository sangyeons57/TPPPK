package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface ReorderTaskLocalUseCase {
    suspend operator fun invoke(
        taskId: DocumentId,
        newOrder: Int,
        projectId: DocumentId
    ): CustomResult<Unit, Exception>
}

class ReorderTaskLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : ReorderTaskLocalUseCase {

    override suspend operator fun invoke(
        taskId: DocumentId,
        newOrder: Int,
        projectId: DocumentId
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 태스크 순서를 재정렬")
    }
} 