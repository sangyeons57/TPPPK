package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import java.time.Instant

/**
 * Event indicating that a project's name has been changed.
 */
data class ProjectNameChangedEvent(
    val projectId: DocumentId,
    val oldName: Name,
    val newName: Name,
    override val occurredOn: Instant
) : DomainEvent
