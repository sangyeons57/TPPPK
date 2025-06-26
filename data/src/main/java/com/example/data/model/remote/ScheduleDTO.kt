package com.example.data.model.remote

import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.base.Schedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO

import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.google.firebase.firestore.PropertyName

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
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
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
        const val CREATED_AT = Schedule.KEY_CREATED_AT
        const val UPDATED_AT = Schedule.KEY_UPDATED_AT
        fun from(entity: Schedule): ScheduleDTO {
            return ScheduleDTO(
                id = entity.id.value,
                title = entity.title.value,
                content = entity.content.value,
                startTime = DateTimeUtil.instantToFirebaseTimestamp(entity.startTime),
                endTime = DateTimeUtil.instantToFirebaseTimestamp(entity.endTime),
                projectId = entity.projectId?.value,
                creatorId = entity.creatorId.value,
                status = entity.status,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(entity.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(entity.updatedAt)
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return Schedule 도메인 모델
     */
    override fun toDomain(): Schedule {
        requireNotNull(startTime) { "startTime is null in ScheduleDTO with id=$id" }
        requireNotNull(endTime) { "endTime is null in ScheduleDTO with id=$id" }
        requireNotNull(createdAt) { "createdAt is null in ScheduleDTO with id=$id" }
        requireNotNull(updatedAt) { "updatedAt is null in ScheduleDTO with id=$id" }

        return Schedule.create(
            title = ScheduleTitle(title),
            content = ScheduleContent(content),
            startTime = DateTimeUtil.firebaseTimestampToInstant(startTime),
            endTime = DateTimeUtil.firebaseTimestampToInstant(endTime),
            projectId = ProjectId(projectId?:""),
            creatorId = OwnerId(creatorId),
            status = status,
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
