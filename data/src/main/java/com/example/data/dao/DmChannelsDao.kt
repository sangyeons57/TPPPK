package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.DmChannelsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for DM Channels collection
 * Provides CRUD operations for DM channel data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface DmChannelsDao {

    // === Basic CRUD Operations ===

    @Query("SELECT * FROM dm_channels WHERE id = :channelId")
    suspend fun getDmChannelById(channelId: String): DmChannelsEntity?

    @Query("SELECT * FROM dm_channels WHERE participants LIKE '%' || :userId || '%' ORDER BY updatedAt DESC")
    suspend fun getDmChannelsByUser(userId: String): List<DmChannelsEntity>

    @Query("SELECT * FROM dm_channels WHERE (participants LIKE '%' || :user1Id || '%') AND (participants LIKE '%' || :user2Id || '%') LIMIT 1")
    suspend fun getDmChannelBetweenUsers(user1Id: String, user2Id: String): DmChannelsEntity?

    @Query("SELECT * FROM dm_channels WHERE isActive = :isActive ORDER BY updatedAt DESC")
    suspend fun getDmChannelsByActiveStatus(isActive: Boolean): List<DmChannelsEntity>

    @Query("SELECT * FROM dm_channels ORDER BY updatedAt DESC")
    suspend fun getAllDmChannels(): List<DmChannelsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDmChannel(channel: DmChannelsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDmChannels(channels: List<DmChannelsEntity>)

    @Update
    suspend fun updateChannel(channel: DmChannelsEntity)

    @Query("DELETE FROM dm_channels WHERE id = :channelId")
    suspend fun deleteDmChannel(channelId: String)

    @Query("DELETE FROM dm_channels WHERE participants LIKE '%' || :userId || '%'")
    suspend fun deleteDmChannelsByUser(userId: String)

    // === Flow-based Real-time Observations ===

    @Query("SELECT * FROM dm_channels WHERE participants LIKE '%' || :userId || '%' ORDER BY updatedAt DESC")
    fun observeDmChannelsByUser(userId: String): Flow<List<DmChannelsEntity>>

    @Query("SELECT * FROM dm_channels WHERE id = :channelId")
    fun observeDmChannelById(channelId: String): Flow<DmChannelsEntity?>

    @Query("SELECT * FROM dm_channels ORDER BY updatedAt DESC")
    fun observeAllDmChannels(): Flow<List<DmChannelsEntity>>

    // === Incremental Sync Queries ===

    @Query("SELECT * FROM dm_channels WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getDmChannelsUpdatedAfter(timestamp: Instant): List<DmChannelsEntity>

    @Query("SELECT MAX(updatedAt) FROM dm_channels")
    suspend fun getLatestUpdateTimestamp(): Instant?

    // === Utility Queries ===

    @Query("SELECT EXISTS(SELECT 1 FROM dm_channels WHERE id = :channelId)")
    suspend fun dmChannelExists(channelId: String): Boolean

    @Query("SELECT COUNT(*) FROM dm_channels WHERE participants LIKE '%' || :userId || '%'")
    suspend fun getDmChannelCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM dm_channels")
    suspend fun getTotalDmChannelCount(): Int

    @Query("SELECT COUNT(*) FROM dm_channels WHERE isActive = 1")
    suspend fun getActiveDmChannelCount(): Int

    // === Cleanup Operations ===

    @Query("DELETE FROM dm_channels")
    suspend fun deleteAllDmChannels()
}