package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.DmWrapperEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for DM Wrapper collection
 * Provides CRUD operations for DM wrapper data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface DmWrapperDao {

    // === Basic CRUD Operations ===

    @Query("SELECT * FROM dm_wrapper WHERE id = :wrapperId")
    suspend fun getDMWrapperById(wrapperId: String): DmWrapperEntity?

    @Query("SELECT * FROM dm_wrapper WHERE otherUserId = :userId ORDER BY updatedAt DESC")
    suspend fun getDMWrappersByUser(userId: String): List<DmWrapperEntity>

    @Query("SELECT * FROM dm_wrapper WHERE dmChannelId = :dmChannelId ORDER BY updatedAt DESC")
    suspend fun getDMWrappersByChannel(dmChannelId: String): List<DmWrapperEntity>

    @Query("SELECT * FROM dm_wrapper WHERE otherUserId = :userId AND dmChannelId = :dmChannelId LIMIT 1")
    suspend fun getDMWrapperByUserAndChannel(userId: String, dmChannelId: String): DmWrapperEntity?

    @Query("SELECT * FROM dm_wrapper WHERE otherUserId = :userId AND unreadCount > 0 ORDER BY updatedAt DESC")
    suspend fun getUnreadDMWrappersByUser(userId: String): List<DmWrapperEntity>

    @Query("SELECT * FROM dm_wrapper ORDER BY updatedAt DESC")
    suspend fun getAllDMWrappers(): List<DmWrapperEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDMWrapper(wrapper: DmWrapperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDMWrappers(wrappers: List<DmWrapperEntity>)

    @Update
    suspend fun updateWrapper(wrapper: DmWrapperEntity)

    @Query("DELETE FROM dm_wrapper WHERE id = :wrapperId")
    suspend fun deleteDMWrapper(wrapperId: String)

    @Query("DELETE FROM dm_wrapper WHERE otherUserId = :userId")
    suspend fun deleteDMWrappersByUser(userId: String)

    @Query("DELETE FROM dm_wrapper WHERE dmChannelId = :dmChannelId")
    suspend fun deleteDMWrappersByChannel(dmChannelId: String)

    // === Flow-based Real-time Observations ===

    @Query("SELECT * FROM dm_wrapper WHERE otherUserId = :userId ORDER BY updatedAt DESC")
    fun observeDMWrappersByUser(userId: String): Flow<List<DmWrapperEntity>>

    @Query("SELECT * FROM dm_wrapper WHERE id = :wrapperId")
    fun observeDMWrapperById(wrapperId: String): Flow<DmWrapperEntity?>

    @Query("SELECT * FROM dm_wrapper WHERE otherUserId = :userId AND unreadCount > 0 ORDER BY updatedAt DESC")
    fun observeUnreadDMWrappersByUser(userId: String): Flow<List<DmWrapperEntity>>

    // === Incremental Sync Queries ===

    @Query("SELECT * FROM dm_wrapper WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getDMWrappersUpdatedAfter(timestamp: Instant): List<DmWrapperEntity>

    @Query("SELECT MAX(updatedAt) FROM dm_wrapper")
    suspend fun getLatestUpdateTimestamp(): Instant?

    // === Utility Queries ===

    @Query("SELECT EXISTS(SELECT 1 FROM dm_wrapper WHERE id = :wrapperId)")
    suspend fun dmWrapperExists(wrapperId: String): Boolean

    @Query("SELECT COUNT(*) FROM dm_wrapper WHERE otherUserId = :userId")
    suspend fun getDMWrapperCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM dm_wrapper")
    suspend fun getTotalDMWrapperCount(): Int

    @Query("SELECT COUNT(*) FROM dm_wrapper WHERE otherUserId = :userId AND unreadCount > 0")
    suspend fun getUnreadDMWrapperCountByUser(userId: String): Int

    @Query("UPDATE dm_wrapper SET lastReadAt = :lastReadAt WHERE id = :wrapperId")
    suspend fun updateLastReadAt(wrapperId: String, lastReadAt: Instant)

    // === Cleanup Operations ===

    @Query("DELETE FROM dm_wrapper")
    suspend fun deleteAllDMWrappers()
}