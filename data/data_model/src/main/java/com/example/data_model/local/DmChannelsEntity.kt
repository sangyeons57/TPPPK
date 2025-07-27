package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for DM Channels collection
 * 1:1 correspondence with Firestore 'dm_channels' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "dm_channels",
    indices = [
        Index(value = ["status"]), // For status filtering
        Index(value = ["participants"]), // For participant queries (stored as JSON)
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class DmChannelsEntity(
    @PrimaryKey
    val id: String,

    /**
     * Participants in the DM channel (JSON serialized List<String>)
     * Format: ["userId1", "userId2"]
     */
    val participants: String,

    /**
     * Channel status (ACTIVE, ARCHIVED, BLOCKED, DELETED)
     */
    val status: String = "ACTIVE",

    /**
     * Whether the channel is active
     */
    val isActive: Boolean = true,

    /**
     * Map of blocked users (JSON serialized Map<String, String>)
     * Key: userId who blocked, Value: userId who was blocked
     * Format: {"blocker1": "blocked1", "blocker2": "blocked2"}
     */
    val blockedByMap: String? = null,

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