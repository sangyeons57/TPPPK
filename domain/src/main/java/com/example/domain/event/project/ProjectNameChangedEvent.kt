package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a project's name has been changed.
 */
data class ProjectNameChangedEvent(
    val projectId: DocumentId,
    val oldName: String,
    val newName: String,
    override val occurredOn: Instant
) : DomainEvent
