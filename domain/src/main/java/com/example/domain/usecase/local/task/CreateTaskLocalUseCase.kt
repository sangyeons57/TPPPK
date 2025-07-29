package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface CreateTaskLocalUseCase {
    suspend operator fun invoke(task: Task): CustomResult<Task, Exception>
}

class CreateTaskLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : CreateTaskLocalUseCase {

    override suspend operator fun invoke(task: Task): CustomResult<Task, Exception> {
        return TODO("로컬 저장소에 새 태스크를 생성")
    }
} 