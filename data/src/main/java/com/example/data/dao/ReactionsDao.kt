package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.ReactionsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Reactions collection
 * Provides CRUD operations for reaction data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface ReactionsDao {

    // === Basic CRUD Operations ===

    /**
     * Get reaction by ID
     */
    @Query("SELECT * FROM reactions WHERE id = :reactionId")
    suspend fun getReactionById(reactionId: String): ReactionsEntity?

    /**
     * Get reactions for a specific message
     */
    @Query("SELECT * FROM reactions WHERE messageId = :messageId ORDER BY createdAt ASC")
    suspend fun getReactionsByMessage(messageId: String): List<ReactionsEntity>

    /**
     * Get reactions by user
     */
    @Query("SELECT * FROM reactions WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getReactionsByUser(userId: String): List<ReactionsEntity>

    /**
     * Get specific user's reaction to a message
     */
    @Query("SELECT * FROM reactions WHERE messageId = :messageId AND userId = :userId AND emoji = :emoji")
    suspend fun getUserReactionToMessage(
        messageId: String,
        userId: String,
        emoji: String
    ): ReactionsEntity?

    /**
     * Insert or update reaction
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: ReactionsEntity)

    /**
     * Insert or update multiple reactions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReactions(reactions: List<ReactionsEntity>)

    /**
     * Update reaction
     */
    @Update
    suspend fun updateReaction(reaction: ReactionsEntity)

    /**
     * Delete reaction by ID
     */
    @Query("DELETE FROM reactions WHERE id = :reactionId")
    suspend fun deleteReaction(reactionId: String)

    /**
     * Delete specific user's reaction to a message
     */
    @Query("DELETE FROM reactions WHERE messageId = :messageId AND userId = :userId AND emoji = :emoji")
    suspend fun deleteUserReaction(messageId: String, userId: String, emoji: String)

    // === Flow-based Real-time Observations ===

    /**
     * Observe reactions for a message
     */
    @Query("SELECT * FROM reactions WHERE messageId = :messageId ORDER BY createdAt ASC")
    fun observeReactionsByMessage(messageId: String): Flow<List<ReactionsEntity>>

    /**
     * Observe reaction by ID
     */
    @Query("SELECT * FROM reactions WHERE id = :reactionId")
    fun observeReactionById(reactionId: String): Flow<ReactionsEntity?>

    /**
     * Observe reactions by user
     */
    @Query("SELECT * FROM reactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeReactionsByUser(userId: String): Flow<List<ReactionsEntity>>

    /**
     * Observe reactions by emoji
     */
    @Query("SELECT * FROM reactions WHERE emoji = :emoji ORDER BY createdAt DESC")
    fun observeReactionsByEmoji(emoji: String): Flow<List<ReactionsEntity>>

    // === Incremental Sync Queries ===

    /**
     * Get reactions updated after specific timestamp (for incremental sync)
     */
    @Query("SELECT * FROM reactions WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getReactionsUpdatedAfter(timestamp: Instant): List<ReactionsEntity>

    /**
     * Get latest update timestamp for sync tracking
     */
    @Query("SELECT MAX(updatedAt) FROM reactions")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Aggregation Queries ===

    /**
     * Get reaction counts by emoji for a message
     */
    @Query(
        """
        SELECT emoji, COUNT(*) as count 
        FROM reactions 
        WHERE messageId = :messageId 
        GROUP BY emoji 
        ORDER BY count DESC
    """
    )
    suspend fun getReactionCountsByEmoji(messageId: String): List<EmojiCount>

    /**
     * Get total reaction count for a message
     */
    @Query("SELECT COUNT(*) FROM reactions WHERE messageId = :messageId")
    suspend fun getReactionCountForMessage(messageId: String): Int

    /**
     * Get users who reacted with specific emoji to a message
     */
    @Query("SELECT userId FROM reactions WHERE messageId = :messageId AND emoji = :emoji ORDER BY createdAt ASC")
    suspend fun getUsersWhoReacted(messageId: String, emoji: String): List<String>

    /**
     * Check if user has reacted to a message with any emoji
     */
    @Query("SELECT EXISTS(SELECT 1 FROM reactions WHERE messageId = :messageId AND userId = :userId)")
    suspend fun hasUserReactedToMessage(messageId: String, userId: String): Boolean

    /**
     * Check if user has reacted with specific emoji
     */
    @Query("SELECT EXISTS(SELECT 1 FROM reactions WHERE messageId = :messageId AND userId = :userId AND emoji = :emoji)")
    suspend fun hasUserReactedWithEmoji(messageId: String, userId: String, emoji: String): Boolean

    // === Popular Reactions ===

    /**
     * Get most used emojis
     */
    @Query(
        """
        SELECT emoji, COUNT(*) as count 
        FROM reactions 
        GROUP BY emoji 
        ORDER BY count DESC 
        LIMIT :limit
    """
    )
    suspend fun getMostUsedEmojis(limit: Int): List<EmojiCount>

    /**
     * Get recent reactions
     */
    @Query("SELECT * FROM reactions ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentReactions(limit: Int): List<ReactionsEntity>

    // === Utility Queries ===

    /**
     * Check if reaction exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM reactions WHERE id = :reactionId)")
    suspend fun reactionExists(reactionId: String): Boolean

    /**
     * Get total reaction count
     */
    @Query("SELECT COUNT(*) FROM reactions")
    suspend fun getTotalReactionCount(): Int

    // === Cleanup Operations ===

    /**
     * Delete reactions for a message
     */
    @Query("DELETE FROM reactions WHERE messageId = :messageId")
    suspend fun deleteReactionsByMessage(messageId: String)

    /**
     * Delete reactions by user
     */
    @Query("DELETE FROM reactions WHERE userId = :userId")
    suspend fun deleteReactionsByUser(userId: String)

    /**
     * Delete all reactions (for cache reset)
     */
    @Query("DELETE FROM reactions")
    suspend fun deleteAllReactions()

    /**
     * Delete reactions older than timestamp
     */
    @Query("DELETE FROM reactions WHERE createdAt < :timestamp")
    suspend fun deleteOldReactions(timestamp: Instant): Int
}

/**
 * Data class for emoji count results
 */
data class EmojiCount(
    val emoji: String,
    val count: Int
)