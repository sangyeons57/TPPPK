package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.TasksEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Tasks collection
 * Provides CRUD operations for task data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface TasksDao {

    // === Basic CRUD Operations ===

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TasksEntity?

    @Query("SELECT * FROM tasks WHERE channelId = :channelId ORDER BY `order` ASC")
    suspend fun getTasksByChannel(channelId: String): List<TasksEntity>

    @Query("SELECT * FROM tasks WHERE checkedBy = :userId ORDER BY checkedAt DESC")
    suspend fun getTasksByUser(userId: String): List<TasksEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TasksEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TasksEntity>)

    @Update
    suspend fun updateTask(task: TasksEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    // === Flow-based Real-time Observations ===

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TasksEntity?>

    @Query("SELECT * FROM tasks WHERE channelId = :channelId ORDER BY `order` ASC")
    fun observeTasksByChannel(channelId: String): Flow<List<TasksEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun observeTasksByStatus(status: String): Flow<List<TasksEntity>>

    // === Task Status Management ===

    @Query("UPDATE tasks SET status = :newStatus WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, newStatus: String)

    @Query(
        """
        UPDATE tasks 
        SET status = :newStatus, 
            checkedBy = :checkedBy, 
            checkedAt = :checkedAt,
            updatedAt = :modifiedAt
        WHERE id = :taskId
    """
    )
    suspend fun completeTask(
        taskId: String,
        newStatus: String,
        checkedBy: String,
        checkedAt: Instant,
        modifiedAt: Instant
    )

    @Query("UPDATE tasks SET `order` = :newOrder WHERE id = :taskId")
    suspend fun updateTaskOrder(taskId: String, newOrder: Int)

    // === Incremental Sync Queries ===

    @Query("SELECT * FROM tasks WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getTasksUpdatedAfter(timestamp: Instant): List<TasksEntity>

    @Query("SELECT MAX(updatedAt) FROM tasks")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Filter Queries ===

    @Query("SELECT * FROM tasks WHERE channelId = :channelId AND status = :status ORDER BY `order` ASC")
    suspend fun getTasksByChannelAndStatus(channelId: String, status: String): List<TasksEntity>

    @Query("SELECT * FROM tasks WHERE taskType = :type ORDER BY createdAt DESC")
    suspend fun getTasksByType(type: String): List<TasksEntity>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY checkedAt DESC")
    suspend fun getCompletedTasks(): List<TasksEntity>

    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' ORDER BY `order` ASC")
    suspend fun getPendingTasks(): List<TasksEntity>

    // === Search Queries ===

    @Query("SELECT * FROM tasks WHERE content LIKE '%' || :searchTerm || '%' ORDER BY createdAt DESC")
    suspend fun searchTasksByContent(searchTerm: String): List<TasksEntity>

    // === Order Management ===

    @Query("SELECT MAX(`order`) FROM tasks WHERE channelId = :channelId")
    suspend fun getMaxOrderInChannel(channelId: String): Int?

    @Query("SELECT MIN(`order`) FROM tasks WHERE channelId = :channelId")
    suspend fun getMinOrderInChannel(channelId: String): Int?

    // === Statistics ===

    @Query("SELECT COUNT(*) FROM tasks WHERE channelId = :channelId")
    suspend fun getTaskCountByChannel(channelId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE channelId = :channelId AND status = :status")
    suspend fun getTaskCountByChannelAndStatus(channelId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE checkedBy = :userId")
    suspend fun getTaskCountByUser(userId: String): Int

    @Query(
        """
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed
        FROM tasks 
        WHERE channelId = :channelId
    """
    )
    suspend fun getTaskProgress(channelId: String): TaskProgress

    // === Utility Queries ===

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE id = :taskId)")
    suspend fun taskExists(taskId: String): Boolean

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTaskCount(): Int

    // === Cleanup Operations ===

    @Query("DELETE FROM tasks WHERE channelId = :channelId")
    suspend fun deleteTasksByChannel(channelId: String)

    @Query("DELETE FROM tasks WHERE checkedBy = :userId")
    suspend fun deleteTasksByUser(userId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE status = 'COMPLETED' AND checkedAt < :timestamp")
    suspend fun deleteOldCompletedTasks(timestamp: Instant): Int
}

/**
 * Data class for task progress results
 */
data class TaskProgress(
    val total: Int,
    val completed: Int
) {
    val percentage: Float get() = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
}