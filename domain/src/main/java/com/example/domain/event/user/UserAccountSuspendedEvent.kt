package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Domain event indicating that a user's account has been suspended.
 */
data class UserAccountSuspendedEvent(
    val userId: String,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
