package com.example.data_core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.OutboxEntity
import kotlinx.coroutines.flow.Flow

/**
 * Sync Outbox DAO for synchronization operations
 * Handles all SSOT sync operations across all collections
 * Centralized outbox management for offline-first architecture
 */
@Dao
interface SyncOutboxDao {

    // === Basic CRUD Operations ===

    /**
     * Insert new outbox operation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OutboxEntity)

    /**
     * Insert multiple outbox operations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<OutboxEntity>)

    /**
     * Update existing operation (for retry count, etc.)
     */
    @Update
    suspend fun updateOperation(operation: OutboxEntity)

    /**
     * Delete operation by ID (when sync succeeds)
     */
    @Query("DELETE FROM outbox WHERE id = :operationId")
    suspend fun deleteOperation(operationId: String)

    /**
     * Delete operations by entity (when entity is deleted)
     */
    @Query("DELETE FROM outbox WHERE collectionName = :collectionName AND documentId = :entityId")
    suspend fun deleteOperationsByEntity(collectionName: String, entityId: String)

    // === Query Operations ===

    /**
     * Get all pending operations for a specific collection
     */
    @Query("SELECT * FROM outbox WHERE collectionName = :collectionName ORDER BY localTimestamp ASC")
    suspend fun getPendingOperationsByCollection(collectionName: String): List<OutboxEntity>

    /**
     * Get all pending operations (for bulk sync)
     */
    @Query("SELECT * FROM outbox ORDER BY localTimestamp ASC")
    suspend fun getAllPendingOperations(): List<OutboxEntity>

    /**
     * Get operations for specific entity
     */
    @Query("SELECT * FROM outbox WHERE collectionName = :collectionName AND documentId = :entityId ORDER BY localTimestamp ASC")
    suspend fun getOperationsByEntity(collectionName: String, entityId: String): List<OutboxEntity>

    /**
     * Get failed operations (retries > 0)
     */
    @Query("SELECT * FROM outbox WHERE retries > 0 ORDER BY retries ASC, localTimestamp ASC")
    suspend fun getFailedOperations(): List<OutboxEntity>

    /**
     * Get operations that haven't been retried recently (for exponential backoff)
     */
    @Query("SELECT * FROM outbox WHERE retries > 0 AND localTimestamp < :beforeTimestamp ORDER BY retries ASC")
    suspend fun getRetryableOperations(beforeTimestamp: Long): List<OutboxEntity>

    // === Flow-based Real-time Observations ===

    /**
     * Observe all pending operations
     */
    @Query("SELECT * FROM outbox ORDER BY localTimestamp ASC")
    fun observeAllOperations(): Flow<List<OutboxEntity>>

    /**
     * Observe operations for specific collection
     */
    @Query("SELECT * FROM outbox WHERE collectionName = :collectionName ORDER BY localTimestamp ASC")
    fun observeOperationsByCollection(collectionName: String): Flow<List<OutboxEntity>>

    /**
     * Observe failed operations count
     */
    @Query("SELECT COUNT(*) FROM outbox WHERE retries > 0")
    fun observeFailedOperationCount(): Flow<Int>

    // === Retry Management ===

    /**
     * Increment retry count for failed operation
     */
    @Query("UPDATE outbox SET retries = retries + 1, localTimestamp = :newTimestamp WHERE id = :operationId")
    suspend fun incrementRetries(operationId: String, newTimestamp: Long)

    /**
     * Reset retry count (when operation succeeds after failures)
     */
    @Query("UPDATE outbox SET retries = 0 WHERE id = :operationId")
    suspend fun resetRetries(operationId: String)

    // === Utility Queries ===

    /**
     * Get operation count by collection
     */
    @Query("SELECT COUNT(*) FROM outbox WHERE collectionName = :collectionName")
    suspend fun getOperationCountByCollection(collectionName: String): Int

    /**
     * Get total pending operation count
     */
    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun getTotalOperationCount(): Int

    /**
     * Check if entity has pending operations
     */
    @Query("SELECT EXISTS(SELECT 1 FROM outbox WHERE collectionName = :collectionName AND documentId = :entityId)")
    suspend fun hasEntityPendingOperations(collectionName: String, entityId: String): Boolean

    /**
     * Get oldest operation timestamp
     */
    @Query("SELECT MIN(localTimestamp) FROM outbox")
    suspend fun getOldestOperationTimestamp(): Long?

    // === Cleanup Operations ===

    /**
     * Delete all operations for a collection (for data reset)
     */
    @Query("DELETE FROM outbox WHERE collectionName = :collectionName")
    suspend fun deleteOperationsByCollection(collectionName: String)

    /**
     * Delete operations older than threshold (for maintenance)
     */
    @Query("DELETE FROM outbox WHERE localTimestamp < :beforeTimestamp")
    suspend fun deleteOldOperations(beforeTimestamp: Long)

    /**
     * Delete all operations (for complete reset)
     */
    @Query("DELETE FROM outbox")
    suspend fun deleteAllOperations()
}