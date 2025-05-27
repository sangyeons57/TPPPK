package com.example.domain._repository

import com.example.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 개인 및 프로젝트 스케줄 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ScheduleRepository {
    suspend fun createSchedule(schedule: Schedule): Result<Schedule>
    suspend fun getScheduleDetails(scheduleId: String): Result<Schedule>
    fun getUserSchedulesStream(userId: String, startDateMillis: Long, endDateMillis: Long): Flow<Result<List<Schedule>>>
    fun getProjectSchedulesStream(projectId: String, startDateMillis: Long, endDateMillis: Long): Flow<Result<List<Schedule>>>
    suspend fun updateSchedule(schedule: Schedule): Result<Unit>
    suspend fun deleteSchedule(scheduleId: String, currentUserId: String): Result<Unit>
    suspend fun getScheduleSummaryForMonth(userId: String, year: Int, month: Int): Result<Map<Int, Boolean>>
}
