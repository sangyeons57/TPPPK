package com.example.domain.event

import java.time.Instant

interface DomainEvent {
    val occurredOn: Instant
}
