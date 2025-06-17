package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Domain event indicating that a user's memo was changed.
 */
data class UserMemoChangedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
