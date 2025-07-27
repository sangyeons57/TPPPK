package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Users collection
 * 1:1 correspondence with Firestore 'users' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["updatedAt"]), // For incremental sync
        Index(value = ["accountStatus"]) // For account filtering
    ]
)
data class UsersEntity(
    @PrimaryKey
    val id: String,

    /**
     * User's email address (immutable)
     */
    val email: String,

    /**
     * User's display name
     */
    val name: String,

    /**
     * Timestamp when user consented to terms (immutable)
     */
    val consentTimeStamp: Instant,

    /**
     * Optional user memo/note
     */
    val memo: String? = null,

    /**
     * User's online status (ONLINE, OFFLINE, AWAY, etc.)
     */
    val userStatus: String = "OFFLINE",

    /**
     * FCM token for push notifications
     */
    val fcmToken: String? = null,

    /**
     * Account status (ACTIVE, SUSPENDED, WITHDRAWN)
     */
    val accountStatus: String = "ACTIVE",

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