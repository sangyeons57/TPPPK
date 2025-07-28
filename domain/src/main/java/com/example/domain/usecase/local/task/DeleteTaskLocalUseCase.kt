package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface DeleteTaskLocalUseCase {
    suspend operator fun invoke(taskId: DocumentId): CustomResult<Unit, Exception>
}

class DeleteTaskLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : DeleteTaskLocalUseCase {

    override suspend operator fun invoke(taskId: DocumentId): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 지정된 태스크를 삭제")
    }
} 