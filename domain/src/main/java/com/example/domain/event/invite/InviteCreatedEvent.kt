package com.example.domain.event.invite

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

data class InviteCreatedEvent(
    val inviteId: DocumentId,
    val inviterId: UserId,
    val projectId: DocumentId,
    val inviteeId: UserId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
