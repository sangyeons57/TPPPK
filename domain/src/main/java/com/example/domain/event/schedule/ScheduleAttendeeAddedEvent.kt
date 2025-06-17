package com.example.domain.event.schedule

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when an attendee is added to a schedule.
 *
 * @param scheduleId ID of the schedule.
 * @param attendeeId ID of the attendee that was added.
 * @param occurredOn Event timestamp (UTC).
 */
data class ScheduleAttendeeAddedEvent(
    val scheduleId: String,
    val attendeeId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
