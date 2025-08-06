package com.example.feature_chat.model

import com.example.domain.enum.OutBoxStatus
import com.example.domain.vo.message.MessageType
import java.time.Instant

/**
 * UI-specific message entity that includes both domain information and data-layer sync status
 * Used for Paging3 to display immediate feedback for sending messages
 */
data class MessageUiEntity(
    val id: String,
    val channelId: String,
    val senderId: String,
    val messageType: MessageType,
    val payload: String,
    val replyToMessageId: String?,
    val isDeleted: Boolean,
    val mentions: String, // JSON string
    val createdAt: Instant,
    val updatedAt: Instant,

    // Sync status information from data layer
    val outBoxStatus: OutBoxStatus
)