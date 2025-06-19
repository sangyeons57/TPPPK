package com.example.domain.event.permission

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a new permission has been created.
 */
data class PermissionCreatedEvent(
    val permissionId: DocumentId,
    val name: String,
    val description: String,
    override val occurredOn: Instant
) : DomainEvent
