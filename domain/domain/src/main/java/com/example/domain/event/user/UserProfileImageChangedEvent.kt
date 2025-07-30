package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a user's profile image is removed or changed (to null).
 *
 * @param userId ID of the user whose profile image changed.
 * @param occurredOn Timestamp when the event occurred.
 */
data class UserProfileImageChangedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
