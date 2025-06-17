package com.example.data.model.remote

import com.example.domain.model._new.enum.ScheduleStatus
import com.example.domain.model.base.Schedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.constants.FirestoreConstants
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
    @get:PropertyName(FirestoreConstants.Schedule.TITLE)
    val title: String = "",
    @get:PropertyName(FirestoreConstants.Schedule.CONTENT)
    val content: String = "",
    @get:PropertyName(FirestoreConstants.Schedule.START_TIME)
    val startTime: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Schedule.END_TIME)
    val endTime: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Schedule.PROJECT_ID)
    val projectId: String? = null,
    @get:PropertyName(FirestoreConstants.Schedule.CREATOR_ID)
    val creatorId: String = "",
    @get:PropertyName(FirestoreConstants.Schedule.STATUS)
    val status: String = "CONFIRMED", // "CONFIRMED", "TENTATIVE", "CANCELLED"
    @get:PropertyName(FirestoreConstants.Schedule.COLOR)
    val color: String? = null, // 예: "#FF5733"
    @get:PropertyName(FirestoreConstants.Schedule.CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Schedule.UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return Schedule 도메인 모델
     */
    fun toDomain(): Schedule {
        return Schedule.registerNewSchedule(
            scheduleId = com.example.domain.model.vo.DocumentId(id),
            title = ScheduleTitle(title),
            content = ScheduleContent(content),
            startTime = startTime.let{DateTimeUtil.firebaseTimestampToInstant(it!!)},
            endTime = endTime.let{DateTimeUtil.firebaseTimestampToInstant(it!!)},
            projectId = ProjectId(projectId?:""),
            creatorId = OwnerId(creatorId),
            status = try {
                ScheduleStatus.valueOf(status.uppercase())
            } catch (e: Exception) {
                ScheduleStatus.CONFIRMED
            },
            createdAt = createdAt.let{DateTimeUtil.firebaseTimestampToInstant(it!!)},
            updatedAt = updatedAt.let{DateTimeUtil.firebaseTimestampToInstant(it!!)}
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
        startTime = startTime.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        endTime = endTime.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        projectId = projectId?.value,
        creatorId = creatorId.value,
        status = status.name.lowercase(),
        createdAt = createdAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
    )
}
