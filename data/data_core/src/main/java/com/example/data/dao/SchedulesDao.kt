package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.SchedulesEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Schedules collection
 * Provides CRUD operations for schedule data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface SchedulesDao {

    // === Basic CRUD Operations ===

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: String): SchedulesEntity?

    @Query("SELECT * FROM schedules ORDER BY startTime ASC")
    suspend fun getAllSchedules(): List<SchedulesEntity>

    @Query("SELECT * FROM schedules WHERE projectId = :projectId ORDER BY startTime ASC")
    suspend fun getSchedulesByProject(projectId: String): List<SchedulesEntity>

    @Query("SELECT * FROM schedules WHERE creatorId = :creatorId ORDER BY startTime ASC")
    suspend fun getSchedulesByCreator(creatorId: String): List<SchedulesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: SchedulesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<SchedulesEntity>)

    @Update
    suspend fun updateSchedule(schedule: SchedulesEntity)

    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteSchedule(scheduleId: String)

    // === Flow-based Real-time Observations ===

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    fun observeScheduleById(scheduleId: String): Flow<SchedulesEntity?>

    @Query("SELECT * FROM schedules ORDER BY startTime ASC")
    fun observeAllSchedules(): Flow<List<SchedulesEntity>>

    @Query("SELECT * FROM schedules WHERE projectId = :projectId ORDER BY startTime ASC")
    fun observeSchedulesByProject(projectId: String): Flow<List<SchedulesEntity>>

    @Query("SELECT * FROM schedules WHERE creatorId = :creatorId ORDER BY startTime ASC")
    fun observeSchedulesByCreator(creatorId: String): Flow<List<SchedulesEntity>>

    // === Time-based Queries ===

    @Query(
        """
        SELECT * FROM schedules 
        WHERE startTime >= :startTime AND endTime <= :endTime 
        ORDER BY startTime ASC
    """
    )
    suspend fun getSchedulesInTimeRange(startTime: Instant, endTime: Instant): List<SchedulesEntity>

    @Query(
        """
        SELECT * FROM schedules 
        WHERE startTime >= :startTime AND endTime <= :endTime 
        ORDER BY startTime ASC
    """
    )
    fun observeSchedulesInTimeRange(
        startTime: Instant,
        endTime: Instant
    ): Flow<List<SchedulesEntity>>

    @Query("SELECT * FROM schedules WHERE startTime >= :fromTime ORDER BY startTime ASC")
    suspend fun getUpcomingSchedules(fromTime: Instant): List<SchedulesEntity>

    @Query("SELECT * FROM schedules WHERE endTime < :toTime ORDER BY startTime DESC")
    suspend fun getPastSchedules(toTime: Instant): List<SchedulesEntity>

    @Query(
        """
        SELECT * FROM schedules 
        WHERE startTime <= :currentTime AND endTime >= :currentTime 
        ORDER BY startTime ASC
    """
    )
    suspend fun getCurrentSchedules(currentTime: Instant): List<SchedulesEntity>

    // === Incremental Sync Queries ===

    @Query("SELECT * FROM schedules WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getSchedulesUpdatedAfter(timestamp: Instant): List<SchedulesEntity>

    @Query("SELECT MAX(updatedAt) FROM schedules")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Search and Filter Queries ===

    @Query("SELECT * FROM schedules WHERE title LIKE '%' || :searchTerm || '%' ORDER BY startTime ASC")
    suspend fun searchSchedulesByTitle(searchTerm: String): List<SchedulesEntity>

    @Query("SELECT * FROM schedules WHERE status = :status ORDER BY startTime ASC")
    suspend fun getSchedulesByStatus(status: String): List<SchedulesEntity>

    @Query("SELECT * FROM schedules WHERE projectId IS NULL ORDER BY startTime ASC")
    suspend fun getPersonalSchedules(): List<SchedulesEntity>

    @Query("SELECT * FROM schedules WHERE projectId IS NOT NULL ORDER BY startTime ASC")
    suspend fun getProjectSchedules(): List<SchedulesEntity>

    // === Calendar Queries ===

    @Query(
        """
        SELECT * FROM schedules 
        WHERE startTime >= :dayStart AND startTime < :dayEnd 
        ORDER BY startTime ASC
    """
    )
    suspend fun getSchedulesForDay(dayStart: Instant, dayEnd: Instant): List<SchedulesEntity>

    @Query(
        """
        SELECT * FROM schedules 
        WHERE startTime >= :weekStart AND startTime < :weekEnd 
        ORDER BY startTime ASC
    """
    )
    suspend fun getSchedulesForWeek(weekStart: Instant, weekEnd: Instant): List<SchedulesEntity>

    @Query(
        """
        SELECT * FROM schedules 
        WHERE startTime >= :monthStart AND startTime < :monthEnd 
        ORDER BY startTime ASC
    """
    )
    suspend fun getSchedulesForMonth(monthStart: Instant, monthEnd: Instant): List<SchedulesEntity>

    // === Utility Queries ===

    @Query("SELECT EXISTS(SELECT 1 FROM schedules WHERE id = :scheduleId)")
    suspend fun scheduleExists(scheduleId: String): Boolean

    @Query("SELECT COUNT(*) FROM schedules")
    suspend fun getScheduleCount(): Int

    @Query("SELECT COUNT(*) FROM schedules WHERE projectId = :projectId")
    suspend fun getScheduleCountByProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM schedules WHERE creatorId = :creatorId")
    suspend fun getScheduleCountByCreator(creatorId: String): Int

    // === Cleanup Operations ===

    @Query("DELETE FROM schedules WHERE projectId = :projectId")
    suspend fun deleteSchedulesByProject(projectId: String)

    @Query("DELETE FROM schedules WHERE creatorId = :creatorId")
    suspend fun deleteSchedulesByCreator(creatorId: String)

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()

    @Query("DELETE FROM schedules WHERE endTime < :timestamp")
    suspend fun deleteOldSchedules(timestamp: Instant): Int
}