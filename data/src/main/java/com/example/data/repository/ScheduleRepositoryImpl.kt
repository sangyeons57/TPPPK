// 경로: data/repository/ScheduleRepositoryImpl.kt
package com.example.data.repository

import com.example.domain.model.Schedule
import com.example.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.Result

class ScheduleRepositoryImpl @Inject constructor() : ScheduleRepository {
    override suspend fun getSchedulesForDate(date: LocalDate): Result<List<Schedule>> {
        println("ScheduleRepositoryImpl: getSchedulesForDate called for $date (returning empty list)")
        return Result.success(emptyList())
    }
    override suspend fun getScheduleSummaryForMonth(month: YearMonth): Result<Set<LocalDate>> {
        println("ScheduleRepositoryImpl: getScheduleSummaryForMonth called for $month (returning empty set)")
        return Result.success(emptySet())
    }
    override suspend fun getScheduleDetail(scheduleId: String): Result<Schedule> {
        println("ScheduleRepositoryImpl: getScheduleDetail called for $scheduleId (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
    }
    override suspend fun addSchedule(schedule: Schedule): Result<Unit> {
        println("ScheduleRepositoryImpl: addSchedule called (returning success)")
        return Result.success(Unit)
    }
    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        println("ScheduleRepositoryImpl: deleteSchedule called for $scheduleId (returning success)")
        return Result.success(Unit)
    }
    override suspend fun updateSchedule(schedule: Schedule): Result<Unit> {
        println("ScheduleRepositoryImpl: updateSchedule called for ${schedule.id} (returning success)")
        return Result.success(Unit)
    }
}