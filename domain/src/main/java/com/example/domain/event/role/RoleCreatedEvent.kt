package com.example.domain.event.role

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import java.time.Instant

/**
 * Event indicating that a new role has been created.
 */
data class RoleCreatedEvent(
    val roleId: DocumentId,
    val name: Name,
    val isDefault: RoleIsDefault,
    override val occurredOn: Instant
) : DomainEvent
