package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Local Entity for Schedules collection
 * 1:1 correspondence with Firestore 'schedules' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "schedules",
    indices = [
        Index(value = ["projectId"]), // For project-based filtering
        Index(value = ["creatorId"]), // For creator-based queries
        Index(value = ["startTime"]), // For time-based ordering
        Index(value = ["endTime"]), // For time range queries
        Index(value = ["status"]), // For status filtering
        Index(value = ["startTime", "endTime"]), // For time range queries
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class SchedulesEntity(
    @PrimaryKey
    val id: String,

    /**
     * Project ID this schedule belongs to (optional for personal schedules)
     */
    val projectId: String? = null,

    /**
     * Creator user ID
     */
    val creatorId: String,

    /**
     * Schedule title
     */
    val title: String,

    /**
     * Schedule content/description
     */
    val content: String,

    /**
     * Schedule start time (UTC)
     */
    val startTime: Instant,

    /**
     * Schedule end time (UTC)
     */
    val endTime: Instant,

    /**
     * Schedule status (enum ScheduleStatus)
     */
    val status: String,

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