package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Tasks collection
 * 1:1 correspondence with Firestore 'tasks' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["channelId", "order"]), // For channel-based ordering
        Index(value = ["taskType"]), // For type filtering
        Index(value = ["status"]), // For status filtering
        Index(value = ["checkedBy"]), // For user-based queries
        Index(value = ["checkedAt"]), // For completion time ordering
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class TasksEntity(
    @PrimaryKey
    val id: String,

    /**
     * Channel ID this task belongs to
     */
    val channelId: String,

    /**
     * Task type (CHECKLIST, etc.)
     */
    val taskType: String = "CHECKLIST",

    /**
     * Task status (PENDING, IN_PROGRESS, COMPLETED)
     */
    val status: String = "PENDING",

    /**
     * Task content/description
     */
    val content: String,

    /**
     * Display order within channel
     */
    val order: Int = 0,

    /**
     * User ID who checked/completed this task (optional)
     */
    val checkedBy: String? = null,

    /**
     * When this task was checked/completed (optional)
     */
    val checkedAt: Instant? = null,

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