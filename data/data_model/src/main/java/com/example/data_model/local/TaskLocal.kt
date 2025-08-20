package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["channelId", "order"]),
        Index(value = ["channelId", "updatedAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "channelId")
    val channelId: String,

    @ColumnInfo(name = "taskType")
    val taskType: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "content")
    val content: String,

    // order is a keyword; quote in index above and map here safely
    @ColumnInfo(name = "order")
    val order: Int,

    @ColumnInfo(name = "checkedBy")
    val checkedBy: String?,

    @ColumnInfo(name = "checkedAt")
    val checkedAt: Long?,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,
)

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskEntity)

    @Query(
        """
        SELECT * FROM tasks
        WHERE channelId = :channelId
        ORDER BY `order` ASC, updatedAt DESC
        """
    )
    suspend fun getTasksByChannel(channelId: String): List<TaskEntity>

    /**
     * Observe tasks that belong to a project (composite channelId prefix: "$projectId:")
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE channelId LIKE :projectId || ':%'
        ORDER BY channelId ASC, `order` ASC, updatedAt DESC
        """
    )
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>

    /**
     * Observe tasks by exact channel id (project channel or composite id)
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE channelId = :channelId
        ORDER BY `order` ASC, updatedAt DESC
        """
    )
    fun observeByChannel(channelId: String): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE channelId = :channelId")
    suspend fun clearTasksByChannel(channelId: String): Int

    @Query("DELETE FROM tasks")
    suspend fun clearAll(): Int

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun findById(taskId: String): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String): Int
}
