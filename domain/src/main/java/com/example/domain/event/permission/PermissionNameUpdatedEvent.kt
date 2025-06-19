package com.example.domain.event.permission

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a permission's name has been updated.
 */
data class PermissionNameUpdatedEvent(
    val permissionId: DocumentId,
    val newName: String,
    override val occurredOn: Instant
) : DomainEvent
