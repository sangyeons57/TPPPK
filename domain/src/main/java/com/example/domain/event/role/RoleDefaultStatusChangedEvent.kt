package com.example.domain.event.role

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a role's default status has been changed.
 */
data class RoleDefaultStatusChangedEvent(
    val roleId: DocumentId,
    val newDefaultStatus: Boolean,
    override val occurredOn: Instant
) : DomainEvent
