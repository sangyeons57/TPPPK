package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.project.ProjectStatus
import java.time.Instant

/**
 * Event indicating that a project's status has been changed.
 */
data class ProjectStatusChangedEvent(
    val projectId: DocumentId,
    val oldStatus: ProjectStatus,
    val newStatus: ProjectStatus,
    override val occurredOn: Instant
) : DomainEvent