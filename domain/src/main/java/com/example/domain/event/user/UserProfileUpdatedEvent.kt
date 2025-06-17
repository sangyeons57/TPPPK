package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Domain event indicating that a user's profile (name or image) was updated.
 */
data class UserProfileUpdatedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
