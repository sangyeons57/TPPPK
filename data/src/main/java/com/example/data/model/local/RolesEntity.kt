package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Local Entity for Roles collection
 * 1:1 correspondence with Firestore 'roles' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "roles",
    indices = [
        Index(value = ["projectId"]), // For project-based queries
        Index(value = ["name"]), // For role search
        Index(value = ["isDefault"]), // For default role filtering
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class RolesEntity(
    @PrimaryKey
    val id: String,

    /**
     * Project ID this role belongs to
     */
    val projectId: String,

    /**
     * Role name
     */
    val name: String,

    /**
     * Whether this is the default role for new members
     */
    val isDefault: Boolean = false,

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
) {
    companion object {
        /**
         * System role: Project owner
         */
        const val OWNER = "OWNER"
    }
}