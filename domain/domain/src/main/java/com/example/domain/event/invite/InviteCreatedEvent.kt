package com.example.domain.event.invite

import com.example.domain.event.DomainEvent
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ProjectId
import com.example.domain.vo.UserId
import java.time.Instant

data class InviteCreatedEvent(
    val inviteId: DocumentId,
    val inviterId: UserId,
    val projectId: ProjectId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
