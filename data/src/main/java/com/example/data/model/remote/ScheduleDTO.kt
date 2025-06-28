package com.example.data.model.remote

import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.base.Schedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot

import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.google.firebase.firestore.PropertyName
import com.example.domain.model.vo.DocumentId as VODocumentId
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
    /**
     * DTO를 도메인 모델로 변환
     * @return Schedule 도메인 모델
     */
    override fun toDomain(): Schedule {
        requireNotNull(startTime) { "startTime is null in ScheduleDTO with id=$id" }
        requireNotNull(endTime) { "endTime is null in ScheduleDTO with id=$id" }

        return Schedule.fromDataSource(
            id = VODocumentId(id),
            title = ScheduleTitle(title),
            content = ScheduleContent(content),
            startTime = startTime.toInstant(),
            endTime = endTime.toInstant(),
            projectId = ProjectId(projectId?:""),
            creatorId = OwnerId(creatorId),
            status = status,
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant(),
        )
    }
}

/**
 * Schedule 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ScheduleDTO 객체
 */
fun Schedule.toDto(): ScheduleDTO {
    return ScheduleDTO(
        id = id.value,
        title = title.value,
        content = content.value,
        startTime = Date.from(startTime),
        endTime = Date.from(endTime),
        projectId = projectId?.value,
        creatorId = creatorId.value,
        status = status,
        createdAt = Date.from(createdAt),
        updatedAt = Date.from(updatedAt),
    )
}
