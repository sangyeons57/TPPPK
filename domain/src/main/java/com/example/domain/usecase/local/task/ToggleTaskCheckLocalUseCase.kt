package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface ToggleTaskCheckLocalUseCase {
    suspend operator fun invoke(taskId: DocumentId): CustomResult<Unit, Exception>
}

class ToggleTaskCheckLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : ToggleTaskCheckLocalUseCase {

    override suspend operator fun invoke(taskId: DocumentId): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 태스크 체크 상태를 토글")
    }
} 