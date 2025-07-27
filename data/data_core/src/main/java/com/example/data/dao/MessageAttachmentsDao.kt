package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.MessageAttachmentsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Message Attachments collection
 * Provides CRUD operations for message attachment data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface MessageAttachmentsDao {

    // === Basic CRUD Operations ===

    /**
     * Get attachment by ID
     */
    @Query("SELECT * FROM message_attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): MessageAttachmentsEntity?

    /**
     * Get attachments for a specific message
     */
    @Query("SELECT * FROM message_attachments WHERE messageId = :messageId ORDER BY createdAt ASC")
    suspend fun getAttachmentsByMessage(messageId: String): List<MessageAttachmentsEntity>

    /**
     * Get all attachments
     */
    @Query("SELECT * FROM message_attachments ORDER BY createdAt DESC")
    suspend fun getAllAttachments(): List<MessageAttachmentsEntity>

    /**
     * Insert or update attachment
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: MessageAttachmentsEntity)

    /**
     * Insert or update multiple attachments
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<MessageAttachmentsEntity>)

    /**
     * Update attachment
     */
    @Update
    suspend fun updateAttachment(attachment: MessageAttachmentsEntity)

    /**
     * Delete attachment by ID
     */
    @Query("DELETE FROM message_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    // === Flow-based Real-time Observations ===

    /**
     * Observe attachments for a message
     */
    @Query("SELECT * FROM message_attachments WHERE messageId = :messageId ORDER BY createdAt ASC")
    fun observeAttachmentsByMessage(messageId: String): Flow<List<MessageAttachmentsEntity>>

    /**
     * Observe attachment by ID
     */
    @Query("SELECT * FROM message_attachments WHERE id = :attachmentId")
    fun observeAttachmentById(attachmentId: String): Flow<MessageAttachmentsEntity?>

    /**
     * Observe attachments by upload status
     */
    @Query("SELECT * FROM message_attachments WHERE uploadStatus = :status ORDER BY createdAt DESC")
    fun observeAttachmentsByUploadStatus(status: String): Flow<List<MessageAttachmentsEntity>>

    // === Incremental Sync Queries ===

    /**
     * Get attachments updated after specific timestamp (for incremental sync)
     */
    @Query("SELECT * FROM message_attachments WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getAttachmentsUpdatedAfter(timestamp: Instant): List<MessageAttachmentsEntity>

    /**
     * Get latest update timestamp for sync tracking
     */
    @Query("SELECT MAX(updatedAt) FROM message_attachments")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Upload Management Queries ===

    /**
     * Get attachments by upload status
     */
    @Query("SELECT * FROM message_attachments WHERE uploadStatus = :status ORDER BY createdAt DESC")
    suspend fun getAttachmentsByUploadStatus(status: String): List<MessageAttachmentsEntity>

    /**
     * Update upload status and progress
     */
    @Query(
        """
        UPDATE message_attachments 
        SET uploadStatus = :status, 
            uploadProgress = :progress,
            updatedAt = :modifiedAt
        WHERE id = :attachmentId
    """
    )
    suspend fun updateUploadStatus(
        attachmentId: String,
        status: String,
        progress: Float,
        modifiedAt: Instant
    )

    /**
     * Get pending uploads
     */
    @Query("SELECT * FROM message_attachments WHERE uploadStatus IN ('PENDING', 'UPLOADING') ORDER BY createdAt ASC")
    suspend fun getPendingUploads(): List<MessageAttachmentsEntity>

    /**
     * Get failed uploads
     */
    @Query("SELECT * FROM message_attachments WHERE uploadStatus = 'FAILED' ORDER BY createdAt DESC")
    suspend fun getFailedUploads(): List<MessageAttachmentsEntity>

    // === Search and Filter Queries ===

    /**
     * Search attachments by filename
     */
    @Query("SELECT * FROM message_attachments WHERE fileName LIKE '%' || :searchTerm || '%' ORDER BY createdAt DESC")
    suspend fun searchAttachmentsByFilename(searchTerm: String): List<MessageAttachmentsEntity>

    /**
     * Get attachments by type
     */
    @Query("SELECT * FROM message_attachments WHERE attachmentType = :type ORDER BY createdAt DESC")
    suspend fun getAttachmentsByType(type: String): List<MessageAttachmentsEntity>

    /**
     * Get large attachments
     */
    @Query("SELECT * FROM message_attachments WHERE fileSize > :sizeThreshold ORDER BY fileSize DESC")
    suspend fun getLargeAttachments(sizeThreshold: Long): List<MessageAttachmentsEntity>

    // === Utility Queries ===

    /**
     * Check if attachment exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM message_attachments WHERE id = :attachmentId)")
    suspend fun attachmentExists(attachmentId: String): Boolean

    /**
     * Get attachment count for message
     */
    @Query("SELECT COUNT(*) FROM message_attachments WHERE messageId = :messageId")
    suspend fun getAttachmentCountForMessage(messageId: String): Int

    /**
     * Get total storage size for attachments
     */
    @Query("SELECT SUM(fileSize) FROM message_attachments WHERE fileSize IS NOT NULL")
    suspend fun getTotalStorageSize(): Long?

    // === Cleanup Operations ===

    /**
     * Delete attachments for a message
     */
    @Query("DELETE FROM message_attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsByMessage(messageId: String)

    /**
     * Delete all attachments (for cache reset)
     */
    @Query("DELETE FROM message_attachments")
    suspend fun deleteAllAttachments()

    /**
     * Delete attachments older than timestamp
     */
    @Query("DELETE FROM message_attachments WHERE createdAt < :timestamp")
    suspend fun deleteOldAttachments(timestamp: Instant): Int
}