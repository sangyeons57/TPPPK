package com.example.domain.model.base

import com.example.domain.model._new.enum.ScheduleStatus
import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * 일정 정보를 나타내는 도메인 모델 클래스
 */
data class Schedule(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val projectId: String? = null,
    val creatorId: String = "",
    val status: ScheduleStatus = ScheduleStatus.CONFIRMED, // "CONFIRMED", "TENTATIVE", "CANCELLED"
    val color: String? = null, // 예: "#FF5733"
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

