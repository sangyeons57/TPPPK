package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import java.time.Instant

data class FriendProfileImageChangedEvent(
    val friendId: DocumentId,
    val newProfileImageUrl: ImageUrl?, // Can be null if image is removed
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
