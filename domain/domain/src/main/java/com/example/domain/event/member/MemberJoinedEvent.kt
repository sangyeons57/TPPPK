package com.example.domain.event.member

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a new member has joined a project.
 */
data class MemberJoinedEvent(
    val memberId: DocumentId, // This is the User's ID
    val initialRoleIds: List<DocumentId>,
    override val occurredOn: Instant
) : DomainEvent
