package com.example.domain.usecase.local.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.repository.local.TaskLocalRepository
import javax.inject.Inject

interface UpdateTaskLocalUseCase {
    suspend operator fun invoke(task: Task): CustomResult<Task, Exception>
}

class UpdateTaskLocalUseCaseImpl @Inject constructor(
    private val taskLocalRepository: TaskLocalRepository
) : UpdateTaskLocalUseCase {

    override suspend operator fun invoke(task: Task): CustomResult<Task, Exception> {
        TODO("Task 내용 update")
    }
} 