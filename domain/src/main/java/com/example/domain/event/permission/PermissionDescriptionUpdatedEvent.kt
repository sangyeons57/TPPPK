package com.example.domain.event.permission

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a permission's description has been updated.
 */
data class PermissionDescriptionUpdatedEvent(
    val permissionId: DocumentId,
    val newDescription: String,
    override val occurredOn: Instant
) : DomainEvent
