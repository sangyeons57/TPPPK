package com.example.feature_task.mapper

import com.example.domain.model.base.Task
import com.example.feature_task.model.TaskUiModel

object TaskMapper {
    
    fun toUiModel(task: Task): TaskUiModel {
        return TaskUiModel(
            id = task.id,
            taskType = task.taskType,
            status = task.status,
            content = task.content,
            order = task.order,
            checkedBy = task.checkedBy,
            checkedAt = task.checkedAt,
            updatedAt = task.updatedAt
        )
    }
    
    fun toDomainModel(taskUiModel: TaskUiModel): Task {
        return Task.fromDataSource(
            id = taskUiModel.id,
            taskType = taskUiModel.taskType,
            status = taskUiModel.status,
            content = taskUiModel.content,
            order = taskUiModel.order,
            checkedBy = taskUiModel.checkedBy,
            checkedAt = taskUiModel.checkedAt,
            createdAt = null,
            updatedAt = taskUiModel.updatedAt
        )
    }
}