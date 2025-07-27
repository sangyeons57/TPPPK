package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for DM Wrapper collection
 * 1:1 correspondence with Firestore 'dm_wrapper' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "dm_wrapper",
    indices = [
        Index(value = ["otherUserId"]), // For other user queries
        Index(value = ["otherUserName"]), // For name search
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class DmWrapperEntity(
    @PrimaryKey
    val id: String,

    /**
     * Other user ID in the DM conversation
     */
    val otherUserId: String,

    /**
     * Other user's display name
     */
    val otherUserName: String,

    /**
     * Other user's profile image URL (optional)
     */
    val otherUserImageUrl: String? = null,

    /**
     * Preview of the last message (optional)
     */
    val lastMessagePreview: String? = null,

    /**
     * DM Channel ID this wrapper is associated with
     */
    val dmChannelId: String,

    /**
     * Number of unread messages
     */
    val unreadCount: Int = 0,

    /**
     * Last time this wrapper was read
     */
    val lastReadAt: Instant? = null,

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