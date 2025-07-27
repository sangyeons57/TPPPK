package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.ProjectsWrapperEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ProjectsWrapperDao {

    @Query("SELECT * FROM projects_wrapper WHERE id = :wrapperId")
    suspend fun getWrapperById(wrapperId: String): ProjectsWrapperEntity?

    @Query("SELECT * FROM projects_wrapper WHERE userId = :userId ORDER BY `order` ASC")
    suspend fun getWrappersByUser(userId: String): List<ProjectsWrapperEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrapper(wrapper: ProjectsWrapperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrappers(wrappers: List<ProjectsWrapperEntity>)

    @Update
    suspend fun updateWrapper(wrapper: ProjectsWrapperEntity)

    @Query("DELETE FROM projects_wrapper WHERE id = :wrapperId")
    suspend fun deleteWrapper(wrapperId: String)

    @Query("SELECT * FROM projects_wrapper WHERE userId = :userId ORDER BY `order` ASC")
    fun observeWrappersByUser(userId: String): Flow<List<ProjectsWrapperEntity>>

    @Query("SELECT * FROM projects_wrapper WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getWrappersUpdatedAfter(timestamp: Instant): List<ProjectsWrapperEntity>


    @Query("UPDATE projects_wrapper SET `order` = :newOrder WHERE id = :wrapperId")
    suspend fun updateWrapperOrder(wrapperId: String, newOrder: Int)

    @Query("SELECT MAX(`order`) FROM projects_wrapper WHERE userId = :userId")
    suspend fun getMaxOrderForUser(userId: String): Int?

    @Query("SELECT COUNT(*) FROM projects_wrapper WHERE userId = :userId")
    suspend fun getWrapperCountForUser(userId: String): Int

    @Query("DELETE FROM projects_wrapper WHERE userId = :userId")
    suspend fun deleteWrappersByUser(userId: String)

    @Query("DELETE FROM projects_wrapper")
    suspend fun deleteAllWrappers()
}