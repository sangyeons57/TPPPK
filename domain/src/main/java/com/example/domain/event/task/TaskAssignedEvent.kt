package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

/**
 * Event indicating that a task has been assigned to a user.
 */
data class TaskAssignedEvent(
    val taskId: DocumentId,
    val assigneeId: UserId,
    val assignedBy: UserId,
    override val occurredOn: Instant
) : DomainEvent