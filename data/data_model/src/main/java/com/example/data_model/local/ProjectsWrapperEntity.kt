package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Projects Wrapper collection
 * 1:1 correspondence with Firestore 'projects_wrapper' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 *
 * Used for user-specific project ordering and display information
 */
@Entity(
    tableName = "projects_wrapper",
    indices = [
        Index(value = ["userId"]), // For user-based queries
        Index(value = ["order"]), // For ordering
        Index(value = ["projectName"]), // For project search
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class ProjectsWrapperEntity(
    @PrimaryKey
    val id: String, // This is the project ID

    /**
     * User ID this wrapper belongs to
     */
    val userId: String,

    /**
     * Display order for this project in user's project list
     */
    val order: Int,

    /**
     * Project name (cached for display)
     */
    val projectName: String,

    /**
     * Project image URL (cached for display, optional)
     */
    val projectImageUrl: String? = null,

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