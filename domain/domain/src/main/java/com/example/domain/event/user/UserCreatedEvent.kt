package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a new user is created (registration or initial persistence).
 *
 * @param userId The identifier of the newly created user.
 * @param occurredOn The time when the user was created.
 */
data class UserCreatedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
