package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.vo.ChannelId
import com.example.domain_repository.base.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 채널의 태스크를 Room Flow로 관찰
 */
interface ObserveChannelTasksUseCase {
    operator fun invoke(channelId: ChannelId): Flow<CustomResult<List<Task>, Exception>>
}

class ObserveChannelTasksUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : ObserveChannelTasksUseCase {
    override fun invoke(channelId: ChannelId): Flow<CustomResult<List<Task>, Exception>> =
        taskRepository.observeByChannel(channelId.value)
}

