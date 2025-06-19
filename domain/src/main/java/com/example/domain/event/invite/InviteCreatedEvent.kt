package com.example.domain.event.invite

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import java.time.Instant

data class InviteCreatedEvent(
    val inviteId: DocumentId,
    val createdBy: OwnerId,
    val initialStatus: InviteStatus,
    val expiresAt: Instant?,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
