package com.example.domain.event.schedule

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when an attendee is removed from a schedule.
 *
 * @param scheduleId ID of the schedule.
 * @param attendeeId ID of the attendee that was removed.
 * @param occurredOn Event timestamp (UTC).
 */
data class ScheduleAttendeeRemovedEvent(
    val scheduleId: String,
    val attendeeId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
