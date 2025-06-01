package com.example.domain.model.base

import com.example.domain.model.enum.ProjectChannelType
import com.google.firebase.firestore.DocumentId
import java.time.Instant

data class ProjectChannel(
    @DocumentId val id: String = "",
    val channelName: String = "",
    val order: Double = 0.0,
    val channelType: ProjectChannelType = ProjectChannelType.MESSAGES, // "MESSAGES", "TASKS" 등
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

