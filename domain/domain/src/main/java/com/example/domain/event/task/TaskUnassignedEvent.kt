package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

/**
 * Event indicating that a task has been unassigned.
 */
data class TaskUnassignedEvent(
    val taskId: DocumentId,
    val previousAssigneeId: UserId,
    override val occurredOn: Instant
) : DomainEvent