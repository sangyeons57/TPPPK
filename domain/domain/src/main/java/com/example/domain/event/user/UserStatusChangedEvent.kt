package com.example.domain.event.user

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.UserStatus
import java.time.Instant

/**
 * Domain event indicating that a user's online status has changed.
 */
data class UserStatusChangedEvent(
    val userId: String,
    val newStatus: UserStatus,
    override val occurredOn: Instant = Instant.now()
) : DomainEvent
