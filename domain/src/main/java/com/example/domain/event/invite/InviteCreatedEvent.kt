package com.example.domain.event.invite

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.invite.InviteId
import java.time.Instant

data class InviteCreatedEvent(
    val inviteId: InviteId,
    val createdBy: DocumentId,
    val initialStatus: InviteStatus,
    val expiresAt: Instant?,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
