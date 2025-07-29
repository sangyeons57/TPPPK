package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface GetTasksLocalUseCase {
    suspend operator fun invoke(projectId: DocumentId): CustomResult<List<Task>, Exception>
}

class GetTasksLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : GetTasksLocalUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<List<Task>, Exception> {
        return TODO("로컬 저장소에서 특정 프로젝트의 태스크 목록 조회")
    }
} 