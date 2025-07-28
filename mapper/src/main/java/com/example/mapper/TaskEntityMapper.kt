package com.example.mapper

import com.example.data_core.model.local.TasksEntity
import com.example.domain.model.base.Task
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface TaskEntityMapper : BaseEntityMapper<Task, TasksEntity>

class TaskEntityMapperImpl @Inject constructor() : TaskEntityMapper {
    override fun toDomain(entity: TasksEntity): Task {
        return Task.fromDataSource(
            id = DocumentId(entity.id),
            taskType = TaskType.valueOf(entity.taskType),
            status = TaskStatus.valueOf(entity.status),
            content = TaskContent(entity.content),
            order = TaskOrder(entity.order),
            checkedBy = entity.checkedBy?.let { UserId(it) },
            checkedAt = entity.checkedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: Task): TasksEntity {
        return TasksEntity(
            id = domain.id.value,
            taskType = domain.taskType.name,
            status = domain.status.name,
            content = domain.content.value,
            order = domain.order.value,
            checkedBy = domain.checkedBy?.value,
            checkedAt = domain.checkedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
