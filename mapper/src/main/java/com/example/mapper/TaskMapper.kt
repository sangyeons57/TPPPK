package com.example.mapper

import com.example.data.model.remote.TaskDTO
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskType
import com.example.mapper.base.BaseMapper

interface TaskMapper : BaseMapper<Task, TaskDTO>

class TaskMapperImpl : TaskMapper {
    override fun fromDto(dto: TaskDTO): Task {
        return Task.fromDataSource(
            id = DocumentId(dto.id),
            taskType = TaskType.fromValue(dto.taskType),
            status = dto.status,
            content = TaskContent(dto.content),
            order = TaskOrder(dto.order),
            checkedBy = dto.checkedBy?.let { UserId(it) },
            checkedAt = dto.checkedAt?.toInstant(),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun toDto(domain: Task): TaskDTO {
        return TaskDTO(
            id = domain.id.value,
            type = TaskDTO.TYPE_TASK,
            taskType = domain.taskType.value,
            status = domain.status,
            content = domain.content.value,
            order = domain.order.value,
            checkedBy = domain.checkedBy?.internalValue,
            checkedAt = domain.checkedAt?.let { java.util.Date.from(it) },
            createdAt = null,
            updatedAt = null
        )
    }

    override fun domainToMap(domain: Task): Map<String, Any?> {
        return mapOf(
            Task.KEY_TASK_TYPE to domain.taskType.value,
            Task.KEY_STATUS to domain.status.value,
            Task.KEY_CONTENT to domain.content.value,
            Task.KEY_ORDER to domain.order.value,
            Task.KEY_CHECKED_BY to domain.checkedBy?.internalValue,
            Task.KEY_CHECKED_AT to domain.checkedAt
        )
    }

    override fun dataToMap(data: TaskDTO): Map<String, Any?> {
        return mapOf(
            Task.KEY_TASK_TYPE to data.taskType,
            Task.KEY_STATUS to data.status,
            Task.KEY_CONTENT to data.content,
            Task.KEY_ORDER to data.order,
            Task.KEY_CHECKED_BY to data.checkedBy,
            Task.KEY_CHECKED_AT to data.checkedAt
        )
    }
}
