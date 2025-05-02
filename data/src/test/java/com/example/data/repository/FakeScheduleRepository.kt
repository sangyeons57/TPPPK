package com.example.data.repository

import com.example.domain.model.Schedule
import com.example.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

/**
 * ScheduleRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 ScheduleRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeScheduleRepository : ScheduleRepository {
    
    // 인메모리 일정 데이터 저장소
    private val schedules = ConcurrentHashMap<String, Schedule>()
    
    // 에러 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 일정 데이터 추가
     */
    fun addScheduleData(schedule: Schedule) {
        schedules[schedule.id] = schedule
    }
    
    /**
     * 테스트를 위해 모든 일정 데이터 초기화
     */
    fun clearSchedules() {
        schedules.clear()
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }
    
    /**
     * 에러 시뮬레이션 확인 및 처리
     */
    private fun <T> simulateErrorIfNeeded(): Result<T>? {
        return if (shouldSimulateError) {
            Result.failure(errorToSimulate)
        } else {
            null
        }
    }

    override suspend fun getSchedulesForDate(date: LocalDate): Result<List<Schedule>> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<List<Schedule>>()?.let { return it }
        
        // 해당 날짜에 속하는 일정 필터링
        val filteredSchedules = schedules.values.filter { schedule ->
            val scheduleStartDate = schedule.startTime.toLocalDate()
            val scheduleEndDate = schedule.endTime.toLocalDate()
            
            // 일정 기간 내에 요청된 날짜가 포함되는지 확인
            (date.isEqual(scheduleStartDate) || date.isAfter(scheduleStartDate)) && 
            (date.isEqual(scheduleEndDate) || date.isBefore(scheduleEndDate))
        }
        
        return Result.success(filteredSchedules)
    }

    override suspend fun getScheduleSummaryForMonth(month: YearMonth): Result<Set<LocalDate>> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Set<LocalDate>>()?.let { return it }
        
        // 해당 월에 일정이 있는 날짜 집합 생성
        val datesWithSchedules = mutableSetOf<LocalDate>()
        
        // 해당 월의 모든 날짜 범위
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()
        
        // 각 일정에 대해 해당 월에 포함되는 날짜 추가
        schedules.values.forEach { schedule ->
            val scheduleStartDate = schedule.startTime.toLocalDate()
            val scheduleEndDate = schedule.endTime.toLocalDate()
            
            // 일정 시작일부터 종료일까지 순회
            var currentDate = scheduleStartDate
            while (!currentDate.isAfter(scheduleEndDate)) {
                // 요청된 월에 포함되는 날짜만 추가
                if (!currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                    datesWithSchedules.add(currentDate)
                }
                currentDate = currentDate.plusDays(1)
            }
        }
        
        return Result.success(datesWithSchedules)
    }

    override suspend fun getScheduleDetail(scheduleId: String): Result<Schedule> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Schedule>()?.let { return it }
        
        // 특정 ID의 일정 조회
        return schedules[scheduleId]?.let {
            Result.success(it)
        } ?: Result.failure(NoSuchElementException("Schedule not found with ID: $scheduleId"))
    }

    override suspend fun addSchedule(schedule: Schedule): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 일정 유효성 검사
        if (schedule.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Schedule title cannot be blank"))
        }
        
        if (schedule.endTime.isBefore(schedule.startTime)) {
            return Result.failure(IllegalArgumentException("End time cannot be before start time"))
        }
        
        // 일정 추가
        schedules[schedule.id] = schedule
        
        return Result.success(Unit)
    }

    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 일정 존재 여부 확인
        if (!schedules.containsKey(scheduleId)) {
            return Result.failure(NoSuchElementException("Schedule not found with ID: $scheduleId"))
        }
        
        // 일정 삭제
        schedules.remove(scheduleId)
        
        return Result.success(Unit)
    }

    override suspend fun updateSchedule(schedule: Schedule): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 일정 존재 여부 확인
        if (!schedules.containsKey(schedule.id)) {
            return Result.failure(NoSuchElementException("Schedule not found with ID: ${schedule.id}"))
        }
        
        // 일정 유효성 검사
        if (schedule.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Schedule title cannot be blank"))
        }
        
        if (schedule.endTime.isBefore(schedule.startTime)) {
            return Result.failure(IllegalArgumentException("End time cannot be before start time"))
        }
        
        // 일정 업데이트
        schedules[schedule.id] = schedule
        
        return Result.success(Unit)
    }
} 