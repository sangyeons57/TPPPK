package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Local Entity for Message Attachments collection
 * 1:1 correspondence with Firestore 'message_attachments' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "message_attachments",
    indices = [
        Index(value = ["messageId"]), // For message-based queries
        Index(value = ["attachmentType"]), // For type filtering
        Index(value = ["uploadStatus"]), // For upload management
        Index(value = ["updatedAt"]), // For incremental sync
        Index(value = ["fileName"]) // For file search
    ]
)
data class MessageAttachmentsEntity(
    @PrimaryKey
    val id: String,

    /**
     * Message ID this attachment belongs to
     */
    val messageId: String,

    /**
     * Attachment type (IMAGE, FILE, VIDEO, AUDIO, etc.)
     */
    val attachmentType: String,

    /**
     * URL to the file in storage
     */
    val attachmentUrl: String,

    /**
     * Original file name (optional)
     */
    val fileName: String? = null,

    /**
     * File size in bytes (optional)
     */
    val fileSize: Long? = null,

    /**
     * Thumbnail URL for images/videos (optional)
     */
    val thumbnailUrl: String? = null,

    /**
     * Upload status (PENDING, UPLOADING, COMPLETED, FAILED)
     */
    val uploadStatus: String = "COMPLETED",

    /**
     * Upload progress (0.0 to 1.0)
     */
    val uploadProgress: Float = 1.0f,

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