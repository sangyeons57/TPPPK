package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Project Channels collection
 * 1:1 correspondence with Firestore 'project_channels' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "project_channels",
    indices = [
        Index(value = ["categoryId", "order"]), // For category-based ordering
        Index(value = ["channelType"]), // For type filtering
        Index(value = ["status"]), // For status filtering
        Index(value = ["updatedAt"]), // For incremental sync
        Index(value = ["channelName"]) // For channel search
    ]
)
data class ProjectChannelsEntity(
    @PrimaryKey
    val id: String,

    /**
     * Channel name
     */
    val channelName: String,

    /**
     * Channel type (TEXT, VOICE, etc.)
     */
    val channelType: String = "TEXT",

    /**
     * Display order within category
     */
    val order: Int = 0,

    /**
     * Channel status (ACTIVE, ARCHIVED, DISABLED, DELETED)
     */
    val status: String = "ACTIVE",

    /**
     * Category ID this channel belongs to
     * Use "NoCategory" for project-level channels
     */
    val categoryId: String,

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