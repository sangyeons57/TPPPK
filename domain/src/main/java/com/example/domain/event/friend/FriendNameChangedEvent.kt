package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.friend.FriendId
import com.example.domain.model.vo.friend.FriendName
import java.time.Instant

data class FriendNameChangedEvent(
    val friendId: FriendId,
    val newName: FriendName,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
