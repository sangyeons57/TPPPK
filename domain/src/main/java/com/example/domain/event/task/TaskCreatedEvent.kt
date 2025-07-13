package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.task.TaskPriority
import com.example.domain.model.vo.task.TaskTitle
import java.time.Instant

/**
 * Event indicating that a new task has been created.
 */
data class TaskCreatedEvent(
    val taskId: DocumentId,
    val title: TaskTitle,
    val priority: TaskPriority,
    val assigneeId: UserId?,
    val creatorId: UserId,
    val projectId: DocumentId,
    val channelId: DocumentId,
    val containerId: DocumentId,
    val dueDate: Instant?,
    override val occurredOn: Instant
) : DomainEvent