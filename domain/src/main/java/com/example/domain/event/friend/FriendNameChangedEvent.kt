package com.example.domain.event.friend

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import java.time.Instant

data class FriendNameChangedEvent(
    val friendId: DocumentId,
    val newName: UserName,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
