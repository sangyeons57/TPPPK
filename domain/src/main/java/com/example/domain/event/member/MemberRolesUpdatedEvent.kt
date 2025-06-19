package com.example.domain.event.member

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a member's roles have been updated.
 */
data class MemberRolesUpdatedEvent(
    val memberId: DocumentId,
    val newRoleIds: List<DocumentId>,
    override val occurredOn: Instant
) : DomainEvent
