package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

data class FriendCreatedEvent(
    val friendId: DocumentId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
