package com.example.domain.event.role

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a new role has been created.
 */
data class RoleCreatedEvent(
    val roleId: DocumentId,
    val name: String,
    val isDefault: Boolean,
    override val occurredOn: Instant
) : DomainEvent
