package com.example.domain.event.permission

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.permission.PermissionDescription
import java.time.Instant

/**
 * Event indicating that a permission's description has been updated.
 */
data class PermissionDescriptionUpdatedEvent(
    val permissionId: DocumentId,
    val newDescription: PermissionDescription,
    override val occurredOn: Instant
) : DomainEvent
