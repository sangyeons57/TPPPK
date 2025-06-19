package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.Name
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
