package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskStatus
import java.time.Instant

/**
 * Event indicating that a task's status has changed.
 */
data class TaskStatusChangedEvent(
    val taskId: DocumentId,
    val oldStatus: TaskStatus,
    val newStatus: TaskStatus,
    override val occurredOn: Instant
) : DomainEvent