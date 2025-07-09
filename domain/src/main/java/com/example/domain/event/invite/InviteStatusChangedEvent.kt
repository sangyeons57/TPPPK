package com.example.domain.event.invite

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.ProjectInvitationStatus
import com.example.domain.model.vo.DocumentId
import java.time.Instant

data class InviteStatusChangedEvent(
    val inviteId: DocumentId,
    val newStatus: ProjectInvitationStatus,
    val oldStatus: ProjectInvitationStatus, // It's often useful to know the previous state
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
