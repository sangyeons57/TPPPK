package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ProjectChannel(
    @DocumentId val id: String = "",
    val channelName: String = "",
    val channelType: ProjectChannelType = ProjectChannelType.MESSAGES, // "MESSAGES", "TASKS" 등
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

