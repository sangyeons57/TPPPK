package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.OwnerId
import java.time.Instant

/**
 * Event indicating that a new project has been created.
 */
data class ProjectCreatedEvent(
    val projectId: DocumentId,
    val ownerId: OwnerId,
    val name: Name,
    override val occurredOn: Instant
) : DomainEvent
