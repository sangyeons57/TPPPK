package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Friends collection
 * 1:1 correspondence with Firestore 'friends' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["status"]), // For status filtering
        Index(value = ["name"]), // For friend search
        Index(value = ["requestedAt"]), // For chronological ordering
        Index(value = ["acceptedAt"]), // For accepted friends ordering
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class FriendsEntity(
    @PrimaryKey
    val id: String,

    /**
     * Friend status (PENDING, REQUESTED, ACCEPTED, DECLINED, BLOCKED, REMOVED)
     */
    val status: String,

    /**
     * When the friend request was sent/received (optional)
     */
    val requestedAt: Instant? = null,

    /**
     * When the friend request was accepted (optional)
     */
    val acceptedAt: Instant? = null,

    /**
     * Friend's display name
     */
    val name: String,

    /**
     * Friend's profile image URL (optional)
     */
    val profileImageUrl: String? = null,

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