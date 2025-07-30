package com.example.mapper.task

import com.example.data_model.remote.TaskDTO
import com.example.domain.model.base.Task
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class TaskMapper @Inject constructor() : DtoMapper<Task, TaskDTO> {

    override fun dtoToDomain(dto: TaskDTO): Task {
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

    override fun domainToDto(domain: Task): TaskDTO {
        return TaskDTO(
            id = domain.id.value,
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