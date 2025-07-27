package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.MessagesEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Messages collection
 * Provides CRUD operations for message data
 * Supports SSOT (Single Source of Truth) pattern
 * Replaces the previous ChatMessageDao with enhanced sync capabilities
 */
@Dao
interface MessagesDao {

    // === Basic CRUD Operations ===

    /**
     * Get message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessagesEntity?

    /**
     * Get latest messages for a channel (initial loading)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND isDeleted = 0
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    suspend fun getLatestMessages(channelId: String, limit: Int): List<MessagesEntity>

    /**
     * Get messages before specific timestamp (pagination)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND createdAt < :beforeTimestamp 
        AND isDeleted = 0
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesBefore(
        channelId: String,
        beforeTimestamp: Instant,
        limit: Int
    ): List<MessagesEntity>

    /**
     * Get messages after specific timestamp (new message checking)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND createdAt > :afterTimestamp 
        AND isDeleted = 0
        ORDER BY createdAt ASC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesAfter(
        channelId: String,
        afterTimestamp: Instant,
        limit: Int
    ): List<MessagesEntity>

    /**
     * Insert or update message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessagesEntity)

    /**
     * Insert or update multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessagesEntity>)

    /**
     * Update message
     */
    @Update
    suspend fun updateMessage(message: MessagesEntity)

    /**
     * Delete message by ID (hard delete)
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // === Flow-based Real-time Observations ===

    /**
     * Observe messages in channel (real-time)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND isDeleted = 0
        ORDER BY createdAt DESC 
        LIMIT :limit
    """
    )
    fun observeMessages(channelId: String, limit: Int): Flow<List<MessagesEntity>>

    /**
     * Observe message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun observeMessageById(messageId: String): Flow<MessagesEntity?>

    /**
     * Observe messages by sender
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE senderId = :senderId 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """
    )
    fun observeMessagesBySender(senderId: String): Flow<List<MessagesEntity>>

    // === Incremental Sync Queries ===

    /**
     * Get messages updated after specific timestamp in channel (for incremental sync)
     * 🔑 Core sync method: fetches messages changed since last sync for a channel
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND updatedAt > :timestamp 
        ORDER BY updatedAt ASC
    """
    )
    suspend fun getChannelMessagesUpdatedAfter(
        channelId: String,
        timestamp: Instant
    ): List<MessagesEntity>

    /**
     * Get all messages updated after specific timestamp (for global sync)
     */
    @Query("SELECT * FROM messages WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getMessagesUpdatedAfter(timestamp: Instant): List<MessagesEntity>

    /**
     * Get latest update timestamp for channel
     */
    @Query("SELECT MAX(updatedAt) FROM messages WHERE channelId = :channelId")
    suspend fun getLatestUpdateTimestampForChannel(channelId: String): Instant?

    /**
     * Get latest update timestamp globally
     */
    @Query("SELECT MAX(updatedAt) FROM messages")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Search and Filter Queries ===

    /**
     * Search messages by content
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND content LIKE '%' || :searchTerm || '%' 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """
    )
    suspend fun searchMessagesInChannel(channelId: String, searchTerm: String): List<MessagesEntity>

    /**
     * Get messages by sender in channel
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND senderId = :senderId 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """
    )
    suspend fun getMessagesBySenderInChannel(
        channelId: String,
        senderId: String
    ): List<MessagesEntity>

    /**
     * Get reply messages for a specific message
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND replyToMessageId = :messageId 
        AND isDeleted = 0
        ORDER BY createdAt ASC
    """
    )
    suspend fun getReplies(channelId: String, messageId: String): List<MessagesEntity>

    /**
     * Get messages with mentions
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE channelId = :channelId 
        AND mentions IS NOT NULL 
        AND mentions != '' 
        AND isDeleted = 0
        ORDER BY createdAt DESC
    """
    )
    suspend fun getMessagesWithMentions(channelId: String): List<MessagesEntity>

    // === Channel Statistics ===

    /**
     * Get message count for channel
     */
    @Query("SELECT COUNT(*) FROM messages WHERE channelId = :channelId AND isDeleted = 0")
    suspend fun getMessageCount(channelId: String): Int

    /**
     * Get oldest message timestamp in channel
     */
    @Query("SELECT MIN(createdAt) FROM messages WHERE channelId = :channelId AND isDeleted = 0")
    suspend fun getOldestMessageTimestamp(channelId: String): Instant?

    /**
     * Get newest message timestamp in channel
     */
    @Query("SELECT MAX(createdAt) FROM messages WHERE channelId = :channelId AND isDeleted = 0")
    suspend fun getNewestMessageTimestamp(channelId: String): Instant?

    // === Utility Queries ===

    /**
     * Check if message exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    suspend fun messageExists(messageId: String): Boolean

    /**
     * Get total message count
     */
    @Query("SELECT COUNT(*) FROM messages WHERE isDeleted = 0")
    suspend fun getTotalMessageCount(): Int

    /**
     * Get message count by sender
     */
    @Query("SELECT COUNT(*) FROM messages WHERE senderId = :senderId AND isDeleted = 0")
    suspend fun getMessageCountBySender(senderId: String): Int

    // === Cleanup Operations ===

    /**
     * Delete old messages in channel (memory management)
     */
    @Query("DELETE FROM messages WHERE channelId = :channelId AND createdAt <= :oldestTimestamp")
    suspend fun deleteOldMessages(channelId: String, oldestTimestamp: Instant): Int

    /**
     * Delete all messages in channel
     */
    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun deleteAllMessagesInChannel(channelId: String)

    /**
     * Delete all messages (for cache reset)
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /**
     * Soft delete message (mark as deleted)
     */
    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String)

    // === Debug Queries ===

    /**
     * Get all messages for debug purposes
     */
    @Query("SELECT * FROM messages ORDER BY channelId, createdAt DESC")
    suspend fun getAllMessagesForDebug(): List<MessagesEntity>

    /**
     * Get all messages in channel for debug purposes
     */
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY createdAt DESC")
    suspend fun getAllMessagesInChannelForDebug(channelId: String): List<MessagesEntity>
}