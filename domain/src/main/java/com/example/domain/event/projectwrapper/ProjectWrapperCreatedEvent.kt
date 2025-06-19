package com.example.domain.event.projectwrapper

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import java.time.Instant

/**
 * Event indicating that a new project channel has been created.
 */
data class ProjectWrapperCreatedEvent(
    val id: DocumentId,
    val order: ProjectWrapperOrder,
    override val occurredOn: Instant
) : DomainEvent
