package com.example.domain.event.projectwrapper

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a new project channel has been created.
 */
data class ProjectWrapperCreatedEvent(
    val id: DocumentId,
    val order: String,
    override val occurredOn: Instant
) : DomainEvent
