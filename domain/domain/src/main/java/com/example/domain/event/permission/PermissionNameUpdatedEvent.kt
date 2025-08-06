package com.example.domain.event.permission

import com.example.domain.event.DomainEvent
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a permission's name has been updated.
 */
data class PermissionNameUpdatedEvent(
    val permissionId: DocumentId,
    val newName: RolePermission,
    override val occurredOn: Instant
) : DomainEvent
