package com.example.domain_usecase.usecase.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.task.TaskContent
import com.example.domain.vo.task.TaskOrder
import com.example.domain.vo.task.TaskType
import com.example.domain_repository.base.TaskRepository
import javax.inject.Inject

/**
 * 새로운 태스크를 생성하는 유스케이스
 */
interface CreateTaskUseCase {
    suspend operator fun invoke(
        content: String,
        taskType: TaskType = TaskType.CHECKLIST,
        order: Int = 0,
        channelId: ChannelId
    ): CustomResult<String, Exception>
}

class CreateTaskUseCaseImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : CreateTaskUseCase {
    
    override suspend operator fun invoke(
        content: String,
        taskType: TaskType,
        order: Int,
        channelId: ChannelId
    ): CustomResult<String, Exception> {
        val id = DocumentId(java.util.UUID.randomUUID().toString())
        val newTask = Task.create(
            id = id,
            channelId = DocumentId(channelId.value), // composite allowed (ProjectId:ChannelId)
            taskType = taskType,
            content = TaskContent(content),
            order = TaskOrder(order)
        )

        taskRepository.createTask(newTask)
        return CustomResult.Success(id.value)
    }
}
