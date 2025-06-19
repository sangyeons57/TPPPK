package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

data class FriendStatusChangedEvent(
    val friendId: DocumentId,
    val newStatus: FriendStatus,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
