package com.example.data.model.remote

import com.example.domain.model._new.enum.ScheduleStatus
import com.example.domain.model.base.Schedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

/**
 * 일정 정보를 나타내는 DTO 클래스
 */
data class ScheduleDTO(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val projectId: String? = null,
    val creatorId: String = "",
    val status: String = "CONFIRMED", // "CONFIRMED", "TENTATIVE", "CANCELLED"
    val color: String? = null, // 예: "#FF5733"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return Schedule 도메인 모델
     */
    fun toDomain(): Schedule {
        return Schedule(
            id = id,
            title = title,
            content = content,
            startTime = startTime?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            endTime = endTime?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            projectId = projectId,
            creatorId = creatorId,
            status = try {
                ScheduleStatus.valueOf(status.uppercase())
            } catch (e: Exception) {
                ScheduleStatus.CONFIRMED
            },
            color = color,
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)}
        )
    }
}

/**
 * Schedule 도메인 모델을 DTO로 변환하는 확장 함수
 * @return ScheduleDTO 객체
 */
fun Schedule.toDto(): ScheduleDTO {
    return ScheduleDTO(
        id = id,
        title = title,
        content = content,
        startTime = startTime?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        endTime = endTime?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        projectId = projectId,
        creatorId = creatorId,
        status = status.name.lowercase(),
        color = color,
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
    )
}
