package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Local Entity for Messages collection
 * 1:1 correspondence with Firestore 'messages' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 *
 * This entity replaces the previous ChatsEntity to align with Firestore naming
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["channelId", "createdAt"]), // For channel-based time ordering
        Index(value = ["channelId", "updatedAt"]), // For incremental sync per channel
        Index(value = ["senderId"]), // For sender-based queries
        Index(value = ["replyToMessageId"]), // For reply threads
        Index(value = ["updatedAt"]), // For global incremental sync
        Index(value = ["isDeleted"]) // For filtering deleted messages
    ]
)
data class MessagesEntity(
    @PrimaryKey
    val id: String,

    /**
     * Channel ID where this message belongs
     * Can be DM channel ID or project channel ID
     */
    val channelId: String,

    /**
     * Message sender user ID
     */
    val senderId: String,

    /**
     * Message content
     */
    val content: String,

    /**
     * Reply target message ID (optional)
     */
    val replyToMessageId: String? = null,

    /**
     * Mention information (JSON serialized List<MentionInfo>)
     * Format: [{"type":"USER","id":"userId","displayName":"userName"}, ...]
     */
    val mentions: String? = null,

    /**
     * Whether this message is deleted (soft delete)
     */
    val isDeleted: Boolean = false,

    // === Base Firestore Fields ===

    /**
     * Message creation time (UTC)
     */
    val createdAt: Instant,

    /**
     * Last modification time in Firestore (UTC)
     * 🔑 Key field for incremental synchronization
     */
    val updatedAt: Instant
)