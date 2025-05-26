
package com.example.data.model._remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ProjectChannelDTO(
    @DocumentId val id: String = "",
    val channelName: String = "",
    val channelType: String = "MESSAGES", // "MESSAGES", "TASKS" 등
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

