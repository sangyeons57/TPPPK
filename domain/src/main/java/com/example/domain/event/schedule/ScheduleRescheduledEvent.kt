package com.example.domain.event.schedule

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a schedule's start or end time changes.
 *
 * @param scheduleId ID of the schedule.
 * @param occurredOn Time when the event occurred (UTC).
 */
data class ScheduleRescheduledEvent(
    val scheduleId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
