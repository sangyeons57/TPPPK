
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ScheduleDTO(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val projectId: String = "",
    val creatorId: String = "",
    val status: String = "CONFIRMED", // "CONFIRMED", "TENTATIVE", "CANCELLED"
    val color: String? = null, // 예: "#FF5733"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

