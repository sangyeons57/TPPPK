package com.example.domain.event.permission

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.permission.PermissionDescription
import java.time.Instant

/**
 * Event indicating that a new permission has been created.
 */
data class PermissionCreatedEvent(
    val permissionId: DocumentId,
    val name: Name,
    val description: PermissionDescription,
    override val occurredOn: Instant
) : DomainEvent
