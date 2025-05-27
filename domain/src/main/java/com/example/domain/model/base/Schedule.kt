package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ScheduleDTO(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val projectId: String = "",
    val creatorId: String = "",
    val status: ScheduleStatus = ScheduleStatus.CONFIRMED, // "CONFIRMED", "TENTATIVE", "CANCELLED"
    val color: String? = null, // 예: "#FF5733"
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

