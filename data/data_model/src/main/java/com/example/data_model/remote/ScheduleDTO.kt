package com.example.data_model.remote

import com.example.domain.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 일정 정보를 나타내는 DTO 클래스
 */
data class ScheduleDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(TITLE)
    val title: String = "",
    @get:PropertyName(CONTENT)
    val content: String = "",
    @get:PropertyName(START_TIME)
    @get:ServerTimestamp val startTime: Date? = null,
    @get:PropertyName(END_TIME)
    @get:ServerTimestamp val endTime: Date? = null,
    @get:PropertyName(PROJECT_ID)
    val projectId: String? = null,
    @get:PropertyName(CREATOR_ID)
    val creatorId: String = "",
    @get:PropertyName(STATUS)
    val status: ScheduleStatus = ScheduleStatus.CONFIRMED, // "CONFIRMED", "TENTATIVE", "CANCELLED"
    @get:PropertyName(COLOR)
    val color: String? = null, // 예: "#FF5733"
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {
        const val COLLECTION_NAME = Schedule.COLLECTION_NAME
        const val TITLE = Schedule.KEY_TITLE
        const val CONTENT = Schedule.KEY_CONTENT
        const val START_TIME = Schedule.KEY_START_TIME
        const val END_TIME = Schedule.KEY_END_TIME
        const val PROJECT_ID = Schedule.KEY_PROJECT_ID
        const val CREATOR_ID = Schedule.KEY_CREATOR_ID
        const val STATUS = Schedule.KEY_STATUS
        const val COLOR = Schedule.KEY_COLOR
    }
}

