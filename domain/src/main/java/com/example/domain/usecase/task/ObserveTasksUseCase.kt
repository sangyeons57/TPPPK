package com.example.domain.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.repository.base.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 태스크 목록을 실시간으로 관찰하는 유스케이스
 */
interface ObserveTasksUseCase {
    operator fun invoke(): Flow<CustomResult<List<Task>, Exception>>
}

class ObserveTasksUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : ObserveTasksUseCase {
    
    override operator fun invoke(): Flow<CustomResult<List<Task>, Exception>> {
        return taskRepository.observeAll()
    }
}