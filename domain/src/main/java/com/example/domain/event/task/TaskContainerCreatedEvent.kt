package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

/**
 * Event indicating that a new task container has been created.
 */
data class TaskContainerCreatedEvent(
    val containerId: DocumentId,
    val creatorId: UserId,
    val projectId: DocumentId,
    val channelId: DocumentId,
    override val occurredOn: Instant
) : DomainEvent