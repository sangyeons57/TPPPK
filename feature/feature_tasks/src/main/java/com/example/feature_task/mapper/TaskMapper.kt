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
            lastModified = task.updatedAt
        )
    }
    
    fun toDomainModel(taskUiModel: TaskUiModel): Task {
        return Task.fromDataSource(
            id = taskUiModel.id,
            taskType = taskUiModel.taskType,
            status = taskUiModel.status,
            content = taskUiModel.content,
            order = taskUiModel.order,
            createdAt = null,
            updatedAt = taskUiModel.lastModified
        )
    }
}