package com.example.data_core.datasource.local

import com.example.domain.model.base.Schedule
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalSchedulesDataSource {

    suspend fun getScheduleById(scheduleId: String): Schedule?
    suspend fun getSchedulesByProject(projectId: String): List<Schedule>
    suspend fun getSchedulesByUser(userId: String): List<Schedule>
    suspend fun getSchedulesByDateRange(startDate: Instant, endDate: Instant): List<Schedule>
    suspend fun getSchedulesByChannel(channelId: String): List<Schedule>
    suspend fun getAllSchedules(): List<Schedule>

    suspend fun saveSchedule(schedule: Schedule)
    suspend fun saveSchedules(schedules: List<Schedule>)
    suspend fun deleteSchedule(scheduleId: String)
    suspend fun deleteSchedulesByProject(projectId: String)

    suspend fun getSchedulesUpdatedAfter(timestamp: Instant): List<Schedule>
    fun observeSchedulesByProject(projectId: String): Flow<List<Schedule>>
    fun observeScheduleById(scheduleId: String): Flow<Schedule?>

    suspend fun addToOutbox(scheduleId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<ScheduleOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)

    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    suspend fun scheduleExists(scheduleId: String): Boolean
    suspend fun getScheduleCount(projectId: String): Int
    suspend fun clearAllSchedules()
}

data class ScheduleOutboxOperation(
    val id: String,
    val scheduleId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)