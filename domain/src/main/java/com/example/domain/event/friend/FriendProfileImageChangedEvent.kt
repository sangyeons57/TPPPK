package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.friend.FriendId
import com.example.domain.model.vo.friend.FriendProfileImageUrl
import java.time.Instant

data class FriendProfileImageChangedEvent(
    val friendId: FriendId,
    val newProfileImageUrl: FriendProfileImageUrl?, // Can be null if image is removed
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
