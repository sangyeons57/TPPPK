package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import kotlinx.coroutines.flow.Flow
import java.time.Month
import java.time.YearMonth

/**
 * 개인 및 프로젝트 스케줄 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ScheduleRepository {
    suspend fun createSchedule(schedule: Schedule): CustomResult<Unit, Exception>
    suspend fun getScheduleDetails(scheduleId: String): CustomResult<Schedule, Exception>
    fun getUserSchedulesStream(userId: String, startDateMillis: Long, endDateMillis: Long): Flow<CustomResult<List<Schedule>, Exception>>
    fun getProjectSchedulesStream(projectId: String, startDateMillis: Long, endDateMillis: Long): Flow<CustomResult<List<Schedule>, Exception>>
    suspend fun updateSchedule(schedule: Schedule): CustomResult<Unit, Exception>
    suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception>
    suspend fun getScheduleSummaryForMonth(userId: String, yearMonth: YearMonth): CustomResult<Map<Int, Boolean>, Exception>
}
