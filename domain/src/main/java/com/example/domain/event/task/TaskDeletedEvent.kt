package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a task has been deleted.
 */
data class TaskDeletedEvent(
    val taskId: DocumentId,
    val containerId: DocumentId,
    val projectId: DocumentId,
    override val occurredOn: Instant
) : DomainEvent