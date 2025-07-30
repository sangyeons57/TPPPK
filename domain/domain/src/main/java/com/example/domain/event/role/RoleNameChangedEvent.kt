package com.example.domain.event.role

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import java.time.Instant

/**
 * Event indicating that a role's name has been changed.
 */
data class RoleNameChangedEvent(
    val roleId: DocumentId,
    val oldName: Name,
    val newName: Name,
    override val occurredOn: Instant
) : DomainEvent
