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
    initialProjectId: ProjectId?,
     initialCreatorId: OwnerId,

    // Mutable properties with private setters
    initialTitle: ScheduleTitle,
    initialContent: ScheduleContent,
    initialStartTime: Instant,
    initialEndTime: Instant,
    initialStatus: ScheduleStatus,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {
    val creatorId: OwnerId = initialCreatorId

    var projectId: ProjectId? = initialProjectId
        private set
    var title: ScheduleTitle = initialTitle
        private set
    var content: ScheduleContent = initialContent
        private set
    var startTime: Instant = initialStartTime
        private set
    var endTime: Instant = initialEndTime
        private set
    var status: ScheduleStatus = initialStatus
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_PROJECT_ID to this.projectId?.value,
            KEY_CREATOR_ID to this.creatorId.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_TITLE to this.title.value,
            KEY_CONTENT to this.content.value,
            KEY_START_TIME to this.startTime,
            KEY_END_TIME to this.endTime,
            KEY_STATUS to this.status,
            KEY_UPDATED_AT to this.updatedAt,
        )
    }

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
                initialProjectId = projectId,
                initialCreatorId = creatorId,
                initialTitle = title,
                initialContent = content,
                initialStartTime = startTime,
                initialEndTime = endTime,
                initialStatus = status,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
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
                initialProjectId = projectId,
                initialCreatorId = creatorId,
                initialTitle = title,
                initialContent = content,
                initialStartTime = startTime,
                initialEndTime = endTime,
                initialStatus = status,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                isNew = false,
            )
            return schedule
        }
    }

}

