package com.example.data.model.remote

import com.example.domain.model._new.enum.ScheduleStatus
import com.example.domain.model.base.Schedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.google.firebase.firestore.PropertyName

/**
 * 일정 정보를 나타내는 DTO 클래스
 */
data class ScheduleDTO(
    @DocumentId val id: String = "",
    @get:PropertyName(TITLE)
    val title: String = "",
    @get:PropertyName(CONTENT)
    val content: String = "",
    @get:PropertyName(START_TIME)
    val startTime: Timestamp? = null,
    @get:PropertyName(END_TIME)
    val endTime: Timestamp? = null,
    @get:PropertyName(PROJECT_ID)
    val projectId: String? = null,
    @get:PropertyName(CREATOR_ID)
    val creatorId: String = "",
    @get:PropertyName(STATUS)
    val status: ScheduleStatus = ScheduleStatus.CONFIRMED, // "CONFIRMED", "TENTATIVE", "CANCELLED"
    @get:PropertyName(COLOR)
    val color: String? = null, // 예: "#FF5733"
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null
) {

    companion object {
        const val COLLECTION_NAME = "schedules"
        const val TITLE = "title"
        const val CONTENT = "content"
        const val START_TIME = "startTime"
        const val END_TIME = "endTime"
        const val PROJECT_ID = "projectId"
        const val CREATOR_ID = "creatorId"
        const val STATUS = "status"
        const val COLOR = "color"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Schedule 도메인 모델
     */
    fun toDomain(): Schedule {
        requireNotNull(startTime) { "startTime is null in ScheduleDTO with id=$id" }
        requireNotNull(endTime) { "endTime is null in ScheduleDTO with id=$id" }
        requireNotNull(createdAt) { "createdAt is null in ScheduleDTO with id=$id" }
        requireNotNull(updatedAt) { "updatedAt is null in ScheduleDTO with id=$id" }

        return Schedule.registerNewSchedule(
            scheduleId = com.example.domain.model.vo.DocumentId(id),
            title = ScheduleTitle(title),
            content = ScheduleContent(content),
            startTime = DateTimeUtil.firebaseTimestampToInstant(startTime),
            endTime = DateTimeUtil.firebaseTimestampToInstant(endTime),
            projectId = ProjectId(projectId?:""),
            creatorId = OwnerId(creatorId),
            status = status,
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt),
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt)
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
        startTime = DateTimeUtil.instantToFirebaseTimestamp(startTime),
        endTime = DateTimeUtil.instantToFirebaseTimestamp(endTime),
        projectId = projectId?.value,
        creatorId = creatorId.value,
        status = status,
        createdAt = DateTimeUtil.instantToFirebaseTimestamp(createdAt),
        updatedAt = DateTimeUtil.instantToFirebaseTimestamp(updatedAt),
    )
}
