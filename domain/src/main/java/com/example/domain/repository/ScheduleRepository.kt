// 경로: domain/repository/ScheduleRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.Schedule
import java.time.LocalDate
import java.time.YearMonth
import kotlin.Result

interface ScheduleRepository {
    /** 특정 날짜의 일정 목록 가져오기 */
    suspend fun getSchedulesForDate(date: LocalDate): Result<List<Schedule>> // CalendarViewModel
    /** 특정 월의 일정 요약 정보 가져오기 (예: 점으로 표시할 날짜 목록) */
    suspend fun getScheduleSummaryForMonth(month: YearMonth): Result<Set<LocalDate>> // CalendarViewModel
    /** 일정 상세 정보 가져오기 */
    suspend fun getScheduleDetail(scheduleId: String): Result<Schedule> // ScheduleDetailViewModel
    /** 일정 추가 */
    suspend fun addSchedule(schedule: Schedule): Result<Unit> // AddScheduleViewModel
    /** 일정 삭제 */
    suspend fun deleteSchedule(scheduleId: String): Result<Unit> // ScheduleDetailViewModel, Calendar24HourViewModel
    /** 일정 수정 */
    suspend fun updateSchedule(schedule: Schedule): Result<Unit> // 필요시 추가
}