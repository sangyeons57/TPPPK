package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskType
import java.time.Instant

/**
 * Event indicating that a new task has been created.
 */
data class TaskCreatedEvent(
    val taskId: DocumentId,
    val taskType: TaskType,
    val content: TaskContent,
    val order: TaskOrder,
    val projectId: DocumentId,
    val channelId: DocumentId,
    override val occurredOn: Instant
) : DomainEvent