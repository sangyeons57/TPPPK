package com.example.data.model.remote.schedule

import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.Schedule // Assuming domain model exists
import com.example.domain.model.ScheduleStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.time.Instant

/**
 * 일정 정보를 표현하는 데이터 전송 객체(DTO)
 * Firebase Firestore의 'schedules' 컬렉션과 매핑됩니다.
 */
data class ScheduleDto(
    @DocumentId
    var scheduleId: String = "",

    @PropertyName(FirestoreConstants.ScheduleFields.TITLE)
    var title: String = "",

    @get:PropertyName(FirestoreConstants.ScheduleFields.CONTENT)
    @set:PropertyName(FirestoreConstants.ScheduleFields.CONTENT)
    var description: String? = null, // Changed from "" to null for consistency

    @get:PropertyName(FirestoreConstants.ScheduleFields.START_TIME)
    @set:PropertyName(FirestoreConstants.ScheduleFields.START_TIME)
    var startTime: Timestamp? = null, // Changed to nullable, default removed

    @get:PropertyName(FirestoreConstants.ScheduleFields.END_TIME)
    @set:PropertyName(FirestoreConstants.ScheduleFields.END_TIME)
    var endTime: Timestamp? = null, // Changed to nullable, default removed

    @get:PropertyName(FirestoreConstants.ScheduleFields.PROJECT_ID)
    @set:PropertyName(FirestoreConstants.ScheduleFields.PROJECT_ID)
    var projectId: String? = null,

    @get:PropertyName(FirestoreConstants.ScheduleFields.CHANNEL_ID) // Added from constants
    @set:PropertyName(FirestoreConstants.ScheduleFields.CHANNEL_ID)
    var channelId: String? = null, // Added field

    @get:PropertyName(FirestoreConstants.ScheduleFields.CREATOR_ID)
    @set:PropertyName(FirestoreConstants.ScheduleFields.CREATOR_ID)
    var creatorId: String = "",

    @get:PropertyName(FirestoreConstants.ScheduleFields.PARTICIPANT_IDS)
    @set:PropertyName(FirestoreConstants.ScheduleFields.PARTICIPANT_IDS)
    var participantIds: List<String> = emptyList(),

    @get:PropertyName(FirestoreConstants.ScheduleFields.STATUS)
    @set:PropertyName(FirestoreConstants.ScheduleFields.STATUS)
    var status: ScheduleStatus = ScheduleStatus.SCHEDULED, // String for DTO, domain model will use enum

    @get:PropertyName(FirestoreConstants.ScheduleFields.COLOR)
    @set:PropertyName(FirestoreConstants.ScheduleFields.COLOR)
    var color: String? = null,

    @get:PropertyName(FirestoreConstants.ScheduleFields.CREATED_AT)
    @set:PropertyName(FirestoreConstants.ScheduleFields.CREATED_AT)
    var createdAt: Timestamp? = null, // Changed to nullable, default removed

    @get:PropertyName(FirestoreConstants.ScheduleFields.UPDATED_AT)
    @set:PropertyName(FirestoreConstants.ScheduleFields.UPDATED_AT)
    var updatedAt: Timestamp? = null // Changed to nullable, default removed
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.ScheduleFields.TITLE to title,
            FirestoreConstants.ScheduleFields.CONTENT to description,
            FirestoreConstants.ScheduleFields.START_TIME to startTime,
            FirestoreConstants.ScheduleFields.END_TIME to endTime,
            FirestoreConstants.ScheduleFields.PROJECT_ID to projectId,
            FirestoreConstants.ScheduleFields.CHANNEL_ID to channelId,
            FirestoreConstants.ScheduleFields.CREATOR_ID to creatorId,
            FirestoreConstants.ScheduleFields.PARTICIPANT_IDS to participantIds,
            FirestoreConstants.ScheduleFields.STATUS to status,
            FirestoreConstants.ScheduleFields.COLOR to color,
            FirestoreConstants.ScheduleFields.CREATED_AT to createdAt,
            FirestoreConstants.ScheduleFields.UPDATED_AT to updatedAt
            // scheduleId is not part of the map, it's the document ID
        ).filterValues { it != null }
    }

    fun toBasicDomainModel(): Schedule {
        // Assumes Schedule domain model exists with corresponding fields.
        // Priority and Status will be string here, converted to Enum by mapper extension.
        return Schedule(
            id = this.scheduleId,
            title = this.title,
            content = this.description,
            startTime = Instant.EPOCH, // Placeholder
            endTime = Instant.EPOCH,   // Placeholder
            projectId = this.projectId,
            creatorId = this.creatorId,
            status = this.status,   // Default string if null
            color = this.color,
            createdAt = Instant.EPOCH, // Placeholder
            updatedAt = Instant.EPOCH  // Placeholder
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, documentId: String): ScheduleDto {
            return ScheduleDto(
                scheduleId = documentId,
                title = map[FirestoreConstants.ScheduleFields.TITLE] as? String ?: "",
                description = map[FirestoreConstants.ScheduleFields.CONTENT] as? String,
                startTime = map[FirestoreConstants.ScheduleFields.START_TIME] as? Timestamp,
                endTime = map[FirestoreConstants.ScheduleFields.END_TIME] as? Timestamp,
                projectId = map[FirestoreConstants.ScheduleFields.PROJECT_ID] as? String,
                channelId = map[FirestoreConstants.ScheduleFields.CHANNEL_ID] as? String,
                creatorId = map[FirestoreConstants.ScheduleFields.CREATOR_ID] as? String ?: "",
                participantIds = (map[FirestoreConstants.ScheduleFields.PARTICIPANT_IDS] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                status = map[FirestoreConstants.ScheduleFields.STATUS] as? ScheduleStatus ?: ScheduleStatus.UNKNOWN,
                color = map[FirestoreConstants.ScheduleFields.COLOR] as? String,
                createdAt = map[FirestoreConstants.ScheduleFields.CREATED_AT] as? Timestamp,
                updatedAt = map[FirestoreConstants.ScheduleFields.UPDATED_AT] as? Timestamp
            )
        }

        fun fromBasicDomainModel(domain: Schedule): ScheduleDto {
            // Assumes Schedule domain model exists.
            // Priority and Status will be converted from Enum to String here.
            return ScheduleDto(
                scheduleId = domain.id,
                title = domain.title,
                description = domain.content,
                startTime = null, // Placeholder
                endTime = null,   // Placeholder
                projectId = domain.projectId,
                creatorId = domain.creatorId,
                status = domain.status,     // Assuming domain.status is String, or domain.status.name if Enum
                color = domain.color,
                createdAt = null, // Placeholder
                updatedAt = null  // Placeholder
            )
        }
    }
}