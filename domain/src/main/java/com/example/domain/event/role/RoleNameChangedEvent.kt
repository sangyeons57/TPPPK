package com.example.domain.event.role

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a role's name has been changed.
 */
data class RoleNameChangedEvent(
    val roleId: DocumentId,
    val oldName: String,
    val newName: String,
    override val occurredOn: Instant
) : DomainEvent
