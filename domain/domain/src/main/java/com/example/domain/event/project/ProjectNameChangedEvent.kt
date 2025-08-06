package com.example.domain.event.project

import com.example.domain.event.DomainEvent
import com.example.domain.vo.DocumentId
import com.example.domain.vo.project.ProjectName
import java.time.Instant

/**
 * Event indicating that a project's name has been changed.
 */
data class ProjectNameChangedEvent(
    val projectId: DocumentId,
    val oldName: ProjectName,
    val newName: ProjectName,
    override val occurredOn: Instant
) : DomainEvent
