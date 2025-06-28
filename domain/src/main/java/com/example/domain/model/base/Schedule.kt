package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.ScheduleStatus
import com.google.firebase.firestore.DocumentId as FirestoreDocumentId
import java.time.Instant
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.model.vo.schedule.ScheduleContent
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
    val projectId: ProjectId?,
    val creatorId: OwnerId,

    // Mutable properties with private setters
    title: ScheduleTitle,
    content: ScheduleContent,
    startTime: Instant,
    endTime: Instant,
    status: ScheduleStatus,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant?,
    override val updatedAt: Instant?,
) : AggregateRoot() {

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_PROJECT_ID to this.projectId,
            KEY_CREATOR_ID to this.creatorId,
            KEY_CREATED_AT to this.createdAt,
            KEY_TITLE to this.title,
            KEY_CONTENT to this.content,
            KEY_START_TIME to this.startTime,
            KEY_END_TIME to this.endTime,
            KEY_STATUS to this.status,
            KEY_UPDATED_AT to this.updatedAt,
        )
    }

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


    /**
     * Updates the schedule's main information.
     */
    fun updateDetails(newTitle: ScheduleTitle, newContent: ScheduleContent) {
        this.title = newTitle
        this.content = newContent
        this.pushDomainEvent(ScheduleDetailsUpdatedEvent(id.value))
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
        this.pushDomainEvent(ScheduleRescheduledEvent(id.value))
    }

    /**
     * Changes the status of the schedule (e.g., CONFIRMED, CANCELLED).
     */
    fun changeStatus(newStatus: ScheduleStatus) {
        this.status = newStatus
        this.pushDomainEvent(ScheduleStatusChangedEvent(id.value))
    }

    companion object {  
        const val COLLECTION_NAME = "schedules"
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_START_TIME = "startTime"
        const val KEY_END_TIME = "endTime"
        const val KEY_PROJECT_ID = "projectId"
        const val KEY_CREATOR_ID = "creatorId"
        const val KEY_STATUS = "status"
        const val KEY_COLOR = "color"

        fun create(
            projectId: ProjectId?,
            creatorId: OwnerId,
            title: ScheduleTitle,
            content: ScheduleContent,
            startTime: Instant,
            endTime: Instant,
            status: ScheduleStatus,
        ): Schedule {
            val schedule = Schedule(
                id = DocumentId.EMPTY,
                projectId = projectId,
                creatorId = creatorId,
                title = title,
                content = content,
                startTime = startTime,
                endTime = endTime,
                status = status,
                createdAt = null,
                updatedAt = null,
                isNew = true,
            )
            return schedule
        }

        /**
         * Reconstructs a User instance from a data source (e.g., database).
         * This method assumes the data is valid as it's coming from a trusted source.
         * Further validation or transformation can be added if necessary.
         */
        fun fromDataSource(
            id: DocumentId,
            projectId: ProjectId?,
            creatorId: OwnerId,
            title: ScheduleTitle,
            content: ScheduleContent,
            startTime: Instant,
            endTime: Instant,
            status: ScheduleStatus,
            createdAt: Instant?,
            updatedAt: Instant?,
        ): Schedule {
            val schedule = Schedule(
                id = id,
                projectId = projectId,
                creatorId = creatorId,
                createdAt = createdAt,
                title = title,
                content = content,
                startTime = startTime,
                endTime = endTime,
                status = status,
                updatedAt = updatedAt,
                isNew = false,
            )
            return schedule
        }
    }

}

