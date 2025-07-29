package com.example.mapper

import com.example.data_core.model.remote.TaskDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskType
import com.example.mapper.base.BaseMapper
import java.util.Date

interface TaskMapper : BaseMapper<Task, TaskDTO>

class TaskMapperImpl : TaskMapper {
    override fun toDomain(dto: TaskDTO): Task {
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
            checkedAt = domain.checkedAt?.let { Date.from(it) },
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

    override fun mapToDto(map: Map<String, Any?>): TaskDTO {
        return TaskDTO(
            id = map["id"] as? String ?: "",
            type = map["type"] as? String ?: TaskDTO.TYPE_TASK,
            taskType = map[Task.KEY_TASK_TYPE] as? String ?: TaskType.ETC.value,
            status = (map[Task.KEY_STATUS] as? String)?.let { TaskStatus.valueOf(it) }
                ?: TaskStatus.PENDING,
            content = map[Task.KEY_CONTENT] as? String ?: "",
            order = map[Task.KEY_ORDER] as? Long ?: 0L,
            checkedBy = map[Task.KEY_CHECKED_BY] as? String,
            checkedAt = (map[Task.KEY_CHECKED_AT] as? Date),
            createdAt = (map[AggregateRoot.KEY_CREATED_AT] as? Date),
            updatedAt = (map[AggregateRoot.KEY_UPDATED_AT] as? Date)
        )
    }
}
