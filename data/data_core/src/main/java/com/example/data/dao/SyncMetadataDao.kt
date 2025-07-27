package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Sync Metadata operations
 * Manages synchronization cursors and timestamps for incremental sync
 * Part of 3-tier client-driven sync architecture
 */
@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE collectionName = :collectionName")
    suspend fun getSyncMetadata(collectionName: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllSyncMetadata(): List<SyncMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: List<SyncMetadataEntity>)

    @Update
    suspend fun updateSyncMetadata(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE collectionName = :collectionName")
    suspend fun deleteSyncMetadata(collectionName: String)

    @Query("SELECT * FROM sync_metadata WHERE collectionName = :collectionName")
    fun observeSyncMetadata(collectionName: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata")
    fun observeAllSyncMetadata(): Flow<List<SyncMetadataEntity>>

    @Query("UPDATE sync_metadata SET lastServerCursor = :cursor WHERE collectionName = :collectionName")
    suspend fun updateServerCursor(collectionName: String, cursor: Long)

    @Query("UPDATE sync_metadata SET lastSuccessfulSync = :timestamp WHERE collectionName = :collectionName")
    suspend fun updateLastSuccessfulSync(collectionName: String, timestamp: Long)

    @Query(
        """
        UPDATE sync_metadata 
        SET lastServerCursor = :cursor, lastSuccessfulSync = :timestamp 
        WHERE collectionName = :collectionName
    """
    )
    suspend fun updateSyncStatus(collectionName: String, cursor: Long, timestamp: Long)

    @Query("SELECT lastServerCursor FROM sync_metadata WHERE collectionName = :collectionName")
    suspend fun getLastServerCursor(collectionName: String): Long?

    @Query("SELECT lastSuccessfulSync FROM sync_metadata WHERE collectionName = :collectionName")
    suspend fun getLastSuccessfulSync(collectionName: String): Long?

    @Query("SELECT COUNT(*) FROM sync_metadata")
    suspend fun getSyncMetadataCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM sync_metadata WHERE collectionName = :collectionName)")
    suspend fun syncMetadataExists(collectionName: String): Boolean

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAllSyncMetadata()

    /**
     * Initialize sync metadata for a collection with default values
     */
    @Query(
        """
        INSERT OR IGNORE INTO sync_metadata (collectionName, lastServerCursor, lastSuccessfulSync) 
        VALUES (:collectionName, 0, 0)
    """
    )
    suspend fun initializeSyncMetadata(collectionName: String)

    /**
     * Get collections that need synchronization (haven't been synced recently)
     * @param maxAgeMs Maximum age in milliseconds since last successful sync
     */
    @Query(
        """
        SELECT * FROM sync_metadata 
        WHERE (lastSuccessfulSync + :maxAgeMs) < :currentTime
        ORDER BY lastSuccessfulSync ASC
    """
    )
    suspend fun getCollectionsNeedingSync(
        maxAgeMs: Long,
        currentTime: Long
    ): List<SyncMetadataEntity>
}