package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.ProjectChannelsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Project Channels collection
 * Provides CRUD operations for project channel data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface ProjectChannelsDao {

    // === Basic CRUD Operations ===

    /**
     * Get channel by ID
     */
    @Query("SELECT * FROM project_channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ProjectChannelsEntity?

    /**
     * Get all channels
     */
    @Query("SELECT * FROM project_channels ORDER BY categoryId, `order` ASC")
    suspend fun getAllChannels(): List<ProjectChannelsEntity>

    /**
     * Get channels by category, ordered by display order
     */
    @Query("SELECT * FROM project_channels WHERE categoryId = :categoryId ORDER BY `order` ASC")
    suspend fun getChannelsByCategory(categoryId: String): List<ProjectChannelsEntity>

    /**
     * Insert or update channel
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ProjectChannelsEntity)

    /**
     * Insert or update multiple channels
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ProjectChannelsEntity>)

    /**
     * Update channel
     */
    @Update
    suspend fun updateChannel(channel: ProjectChannelsEntity)

    /**
     * Delete channel by ID
     */
    @Query("DELETE FROM project_channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    // === Flow-based Real-time Observations ===

    /**
     * Observe channel by ID
     */
    @Query("SELECT * FROM project_channels WHERE id = :channelId")
    fun observeChannelById(channelId: String): Flow<ProjectChannelsEntity?>

    /**
     * Observe all channels ordered by category and display order
     */
    @Query("SELECT * FROM project_channels ORDER BY categoryId, `order` ASC")
    fun observeAllChannels(): Flow<List<ProjectChannelsEntity>>

    /**
     * Observe channels by category
     */
    @Query("SELECT * FROM project_channels WHERE categoryId = :categoryId ORDER BY `order` ASC")
    fun observeChannelsByCategory(categoryId: String): Flow<List<ProjectChannelsEntity>>

    /**
     * Observe channels by status
     */
    @Query("SELECT * FROM project_channels WHERE status = :status ORDER BY categoryId, `order` ASC")
    fun observeChannelsByStatus(status: String): Flow<List<ProjectChannelsEntity>>

    /**
     * Observe channels by type
     */
    @Query("SELECT * FROM project_channels WHERE channelType = :type ORDER BY categoryId, `order` ASC")
    fun observeChannelsByType(type: String): Flow<List<ProjectChannelsEntity>>

    // === Incremental Sync Queries ===

    /**
     * Get channels updated after specific timestamp (for incremental sync)
     * 🔑 Core sync method: fetches channels changed since last sync
     */
    @Query("SELECT * FROM project_channels WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getChannelsUpdatedAfter(timestamp: Instant): List<ProjectChannelsEntity>

    /**
     * Get latest update timestamp for sync tracking
     */
    @Query("SELECT MAX(updatedAt) FROM project_channels")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Search and Filter Queries ===

    /**
     * Search channels by name
     */
    @Query("SELECT * FROM project_channels WHERE channelName LIKE '%' || :searchTerm || '%' ORDER BY channelName ASC")
    suspend fun searchChannelsByName(searchTerm: String): List<ProjectChannelsEntity>

    /**
     * Get channels by status
     */
    @Query("SELECT * FROM project_channels WHERE status = :status ORDER BY categoryId, `order` ASC")
    suspend fun getChannelsByStatus(status: String): List<ProjectChannelsEntity>

    /**
     * Get active channels
     */
    @Query("SELECT * FROM project_channels WHERE status = 'ACTIVE' ORDER BY categoryId, `order` ASC")
    suspend fun getActiveChannels(): List<ProjectChannelsEntity>

    /**
     * Get channels by type
     */
    @Query("SELECT * FROM project_channels WHERE channelType = :type ORDER BY categoryId, `order` ASC")
    suspend fun getChannelsByType(type: String): List<ProjectChannelsEntity>

    // === Channel Ordering Operations ===

    /**
     * Get max order value in category
     */
    @Query("SELECT MAX(`order`) FROM project_channels WHERE categoryId = :categoryId")
    suspend fun getMaxOrderInCategory(categoryId: String): Int?

    /**
     * Update channel order
     */
    @Query("UPDATE project_channels SET `order` = :newOrder WHERE id = :channelId")
    suspend fun updateChannelOrder(channelId: String, newOrder: Int)

    /**
     * Move channel to different category
     */
    @Query("UPDATE project_channels SET categoryId = :newCategoryId, `order` = :newOrder WHERE id = :channelId")
    suspend fun moveChannelToCategory(channelId: String, newCategoryId: String, newOrder: Int)

    // === Utility Queries ===

    /**
     * Check if channel exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM project_channels WHERE id = :channelId)")
    suspend fun channelExists(channelId: String): Boolean

    /**
     * Get total channel count
     */
    @Query("SELECT COUNT(*) FROM project_channels")
    suspend fun getChannelCount(): Int

    /**
     * Get channel count by category
     */
    @Query("SELECT COUNT(*) FROM project_channels WHERE categoryId = :categoryId")
    suspend fun getChannelCountByCategory(categoryId: String): Int

    /**
     * Get channel count by status
     */
    @Query("SELECT COUNT(*) FROM project_channels WHERE status = :status")
    suspend fun getChannelCountByStatus(status: String): Int

    /**
     * Get channel count by type
     */
    @Query("SELECT COUNT(*) FROM project_channels WHERE channelType = :type")
    suspend fun getChannelCountByType(type: String): Int

    // === Cleanup Operations ===

    /**
     * Delete all channels (for cache reset)
     */
    @Query("DELETE FROM project_channels")
    suspend fun deleteAllChannels()

    /**
     * Delete channels with specific status
     */
    @Query("DELETE FROM project_channels WHERE status = :status")
    suspend fun deleteChannelsByStatus(status: String)

    /**
     * Delete channels by category
     */
    @Query("DELETE FROM project_channels WHERE categoryId = :categoryId")
    suspend fun deleteChannelsByCategory(categoryId: String)
}