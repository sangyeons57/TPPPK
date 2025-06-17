package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.friend.FriendId
import java.time.Instant

data class FriendCreatedEvent(
    val friendId: FriendId,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
