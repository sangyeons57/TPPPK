package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.model._new.enum.ScheduleStatus
import com.google.firebase.firestore.DocumentId as FirestoreDocumentId
import java.time.Instant
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.event.DomainEvent
import com.example.domain.event.schedule.ScheduleCreatedEvent
import com.example.domain.event.schedule.ScheduleDetailsUpdatedEvent
import com.example.domain.event.schedule.ScheduleRescheduledEvent
import com.example.domain.event.schedule.ScheduleStatusChangedEvent

/**
 * 일정 정보를 나타내는 도메인 모델 클래스
 */
class Schedule private constructor(
    // Immutable properties
    @FirestoreDocumentId
    val id: DocumentId,
    val projectId: ProjectId?,
    val creatorId: OwnerId,
    val createdAt: Instant,

    // Mutable properties with private setters
    title: ScheduleTitle,
    content: ScheduleContent,
    startTime: Instant,
    endTime: Instant,
    status: ScheduleStatus,
    updatedAt: Instant
) : AggregateRoot {

    /** Collects domain events raised by this aggregate until they are dispatched. */
    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    /**
     * Returns and clears the accumulated domain events.
     */
    override fun pullDomainEvents(): List<DomainEvent> {
        val copy = _domainEvents.toList()
        _domainEvents.clear()
        return copy
    }

    override fun clearDomainEvents() {
        _domainEvents.clear()
    }

    // Exposed mutable properties with restricted setters
    var title: ScheduleTitle = title
        private set

    var content: ScheduleContent = content
        private set

    var startTime: Instant = startTime
        private set

    var endTime: Instant = endTime
        private set

    var status: ScheduleStatus = status
        private set


    var updatedAt: Instant = updatedAt
        private set

    /**
     * Updates the schedule's main information.
     */
    fun updateDetails(newTitle: ScheduleTitle, newContent: ScheduleContent) {
        this.title = newTitle
        this.content = newContent
        this.updatedAt = Instant.now()
        _domainEvents.add(ScheduleDetailsUpdatedEvent(id.value))
    }

    /**
     * Reschedules the event to a new time frame.
     */
    fun reschedule(newStartTime: Instant, newEndTime: Instant) {
        if (newStartTime.isAfter(newEndTime)) {
            throw IllegalArgumentException("Start time must be before end time.")
        }
        this.startTime = newStartTime
        this.endTime = newEndTime
        this.updatedAt = Instant.now()
        _domainEvents.add(ScheduleRescheduledEvent(id.value))
    }

    /**
     * Changes the status of the schedule (e.g., CONFIRMED, CANCELLED).
     */
    fun changeStatus(newStatus: ScheduleStatus) {
        this.status = newStatus
        this.updatedAt = Instant.now()
        _domainEvents.add(ScheduleStatusChangedEvent(id.value))
    }

    companion object {  
        fun registerNewSchedule(
            scheduleId: DocumentId,
            projectId: ProjectId?,
            creatorId: OwnerId,
            createdAt: Instant,
            title: ScheduleTitle,
            content: ScheduleContent,
            startTime: Instant,
            endTime: Instant,
            status: ScheduleStatus,
            updatedAt: Instant,
        ): Schedule {
            val schedule = Schedule(
                id = scheduleId,
                projectId = projectId,
                creatorId = creatorId,
                createdAt = createdAt,
                title = title,
                content = content,
                startTime = startTime,
                endTime = endTime,
                status = status,
                updatedAt = updatedAt,
            )
            schedule._domainEvents.add(ScheduleCreatedEvent(scheduleId.value))
            return schedule
        }

        /**
         * Reconstructs a User instance from a data source (e.g., database).
         * This method assumes the data is valid as it's coming from a trusted source.
         * Further validation or transformation can be added if necessary.
         */
        fun fromDataSource(
            scheduleId: DocumentId,
            projectId: ProjectId?,
            creatorId: OwnerId,
            createdAt: Instant,
            title: ScheduleTitle,
            content: ScheduleContent,
            startTime: Instant,
            endTime: Instant,
            status: ScheduleStatus,
            updatedAt: Instant,
        ): Schedule {
            val schedule = Schedule(
                id = scheduleId,
                projectId = projectId,
                creatorId = creatorId,
                createdAt = createdAt,
                title = title,
                content = content,
                startTime = startTime,
                endTime = endTime,
                status = status,
                updatedAt = updatedAt,
            )
            schedule._domainEvents.add(ScheduleCreatedEvent(scheduleId.value))
            return schedule
        }
    }
}

