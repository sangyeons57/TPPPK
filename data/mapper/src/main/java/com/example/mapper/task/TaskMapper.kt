package com.example.mapper.task

import com.example.data_model.local.TaskEntity
import com.example.data_model.remote.TaskDTO
import com.example.domain.model.base.Task
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.task.TaskContent
import com.example.domain.vo.task.TaskOrder
import com.example.domain.vo.task.TaskType
import com.example.mapper.DtoMapper
import com.example.mapper.Mapper
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class TaskMapper @Inject constructor() : Mapper<TaskEntity, Task, TaskDTO>,
    DtoMapper<Task, TaskDTO> {

    override fun entityToDomain(entity: TaskEntity): Task {
        return Task.fromDataSource(
            id = DocumentId(entity.id),
            channelId = DocumentId(entity.channelId),
            taskType = TaskType.fromValue(entity.taskType),
            status = com.example.domain.vo.task.TaskStatus.fromValue(entity.status),
            content = TaskContent(entity.content),
            order = TaskOrder(entity.order),
            checkedBy = entity.checkedBy?.let { UserId(it) },
            checkedAt = entity.checkedAt?.let { Instant.ofEpochMilli(it) },
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt)
        )
    }

    override fun domainToEntity(domain: Task): TaskEntity {
        return TaskEntity(
            id = domain.id.value,
            channelId = domain.channelId.value,
            taskType = domain.taskType.value,
            status = domain.status.value,
            content = domain.content.value,
            order = domain.order.value,
            checkedBy = domain.checkedBy?.value,
            checkedAt = domain.checkedAt?.toEpochMilli(),
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli(),
        )
    }

    override fun dtoToDomain(dto: TaskDTO): Task {
        return Task.fromDataSource(
            id = DocumentId(dto.id),
            channelId = DocumentId(dto.channelId),
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

    override fun domainToDto(domain: Task): TaskDTO {
        return TaskDTO(
            id = domain.id.value,
            channelId = domain.channelId.value,
            taskType = domain.taskType.value,
            status = domain.status,
            content = domain.content.value,
            order = domain.order.value,
            checkedBy = domain.checkedBy?.value,
            checkedAt = null, // ServerTimestamp가 처리
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}
