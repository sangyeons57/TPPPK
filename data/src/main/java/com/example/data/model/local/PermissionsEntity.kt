package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Local Entity for Permissions collection
 * 1:1 correspondence with Firestore 'permissions' collection (subcollection of roles)
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 *
 * Note: Permission ID is enum-based (RolePermission)
 */
@Entity(
    tableName = "permissions",
    indices = [
        Index(value = ["roleId"]), // For role-based queries
        Index(value = ["projectId"]), // For project-based queries
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class PermissionsEntity(
    @PrimaryKey
    val id: String, // This is the RolePermission enum name

    /**
     * Role ID this permission belongs to
     */
    val roleId: String,

    /**
     * Project ID for easier querying
     */
    val projectId: String,

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