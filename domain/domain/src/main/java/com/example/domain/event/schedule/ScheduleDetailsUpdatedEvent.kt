package com.example.domain.event.schedule

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when a schedule's title, content, or color changes.
 *
 * @param scheduleId ID of the schedule.
 * @param occurredOn Time when the event occurred (UTC).
 */
data class ScheduleDetailsUpdatedEvent(
    val scheduleId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
