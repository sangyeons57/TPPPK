package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Reactions collection
 * 1:1 correspondence with Firestore 'reactions' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "reactions",
    indices = [
        Index(value = ["messageId"]), // For message-based queries
        Index(value = ["userId"]), // For user-based queries  
        Index(value = ["emoji"]), // For emoji filtering
        Index(
            value = ["messageId", "userId", "emoji"],
            unique = true
        ), // Prevent duplicate reactions
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class ReactionsEntity(
    @PrimaryKey
    val id: String,

    /**
     * Message ID this reaction belongs to
     */
    val messageId: String,

    /**
     * User ID who reacted
     */
    val userId: String,

    /**
     * Unicode emoji character (e.g., "👍", "❤️", "😂")
     */
    val emoji: String,

    // === Base Firestore Fields ===

    /**
     * Record creation time (UTC)
     */
    val createdAt: Instant,

    /**
     * Last modification time in Firestore (UTC)
     * 🔑 Key field for incremental synchronization
     */
    val updatedAt: Instant
)