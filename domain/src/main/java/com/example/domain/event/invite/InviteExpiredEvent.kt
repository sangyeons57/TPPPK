package com.example.domain.event.invite

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

data class InviteExpiredEvent(
    val inviteId: DocumentId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
