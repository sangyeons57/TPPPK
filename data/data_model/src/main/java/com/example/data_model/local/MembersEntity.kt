package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Members collection
 * 1:1 correspondence with Firestore 'members' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 *
 * Note: The member ID is the user ID who is a member of the project
 */
@Entity(
    tableName = "members",
    indices = [
        Index(value = ["projectId"]), // For project-based queries
        Index(value = ["roleIds"]), // For role-based filtering (stored as JSON)
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class MembersEntity(
    @PrimaryKey
    val id: String, // This is the user ID

    /**
     * Project ID this member belongs to
     */
    val projectId: String,

    /**
     * Role IDs assigned to this member (JSON serialized List<String>)
     * Format: ["roleId1", "roleId2", "roleId3"]
     */
    val roleIds: String,

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