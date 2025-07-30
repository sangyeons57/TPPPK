package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Domain event indicating that a user's FCM token was updated.
 */
data class UserFcmTokenUpdatedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
