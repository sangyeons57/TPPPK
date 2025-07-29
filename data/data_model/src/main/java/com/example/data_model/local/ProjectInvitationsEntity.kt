package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Project Invitations collection
 * 1:1 correspondence with Firestore 'project_invitations' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 *
 * Note: The ID is the invite code itself (same as document ID)
 */
@Entity(
    tableName = "project_invitations",
    indices = [
        Index(value = ["inviteCode"], unique = true), // Invite code uniqueness
        Index(value = ["projectId"]), // For project-based queries
        Index(value = ["inviterId"]), // For inviter-based queries
        Index(value = ["status"]), // For status filtering
        Index(value = ["expiresAt"]), // For expiration checks
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class ProjectInvitationsEntity(
    @PrimaryKey
    val id: String, // This is the invite code

    /**
     * Invite code (same as ID, but explicit for clarity)
     */
    val inviteCode: String,

    /**
     * Invitation status (ACTIVE, EXPIRED, REVOKED, USED)
     */
    val status: String,

    /**
     * User ID who created this invitation
     */
    val inviterId: String,

    /**
     * Project ID this invitation is for
     */
    val projectId: String,

    /**
     * When this invitation expires (optional)
     */
    val expiresAt: Instant? = null,

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