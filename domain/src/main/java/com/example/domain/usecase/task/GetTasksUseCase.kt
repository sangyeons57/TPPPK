package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.repository.base.TaskRepository
import javax.inject.Inject

/**
 * 태스크 목록을 한 번 조회하는 유스케이스
 */
interface GetTasksUseCase {
    suspend operator fun invoke(): CustomResult<List<Task>, Exception>
}

class GetTasksUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : GetTasksUseCase {
    
    override suspend operator fun invoke(): CustomResult<List<Task>, Exception> {
        return taskRepository.findAll()
    }
}