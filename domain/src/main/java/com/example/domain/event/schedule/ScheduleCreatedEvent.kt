package com.example.domain.event.schedule

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a new schedule is created.
 *
 * @param scheduleId ID of the newly created schedule.
 * @param occurredOn Time when the event occurred (UTC).
 */
data class ScheduleCreatedEvent(
    val scheduleId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
