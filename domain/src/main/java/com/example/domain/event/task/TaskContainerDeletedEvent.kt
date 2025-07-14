package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a task container has been deleted.
 */
data class TaskContainerDeletedEvent(
    val containerId: DocumentId,
    val projectId: DocumentId,
    val channelId: DocumentId,
    override val occurredOn: Instant
) : DomainEvent