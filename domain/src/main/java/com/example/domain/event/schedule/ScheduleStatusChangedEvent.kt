package com.example.domain.event.schedule

import com.example.domain.event.DomainEvent
import java.time.Instant

/**
 * Raised when the status of a schedule changes (e.g., confirmed, cancelled).
 *
 * @param scheduleId ID of the schedule.
 * @param occurredOn Event timestamp (UTC).
 */
data class ScheduleStatusChangedEvent(
    val scheduleId: String,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent
