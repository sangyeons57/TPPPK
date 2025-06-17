package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a user's name (nickname) is changed.
 *
 * @param userId ID of the user whose name changed.
 * @param occurredOn Timestamp when the event occurred.
 */
data class UserNameChangedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
