package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Local Entity for Projects collection
 * 1:1 correspondence with Firestore 'projects' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["ownerId"]), // For owner-based queries
        Index(value = ["status"]), // For status filtering
        Index(value = ["updatedAt"]), // For incremental sync
        Index(value = ["name"]) // For project search
    ]
)
data class ProjectsEntity(
    @PrimaryKey
    val id: String,

    /**
     * Project name
     */
    val name: String,

    /**
     * Project image URL (deprecated - use fixed path system)
     */
    val imageUrl: String? = null,

    /**
     * Project status (ACTIVE, ARCHIVED, DELETED)
     */
    val status: String = "ACTIVE",

    /**
     * Owner user ID (immutable)
     */
    val ownerId: String,

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