package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String? = null,
    val content: String = "", // sendMessage 보다 content가 더 명확한 필드명입니다.
    val sentAt: Instant? = null,
    val updatedAt: Instant? = null,
    val replyToMessageId: String? = null,
    val isDeleted: Boolean = false
)

