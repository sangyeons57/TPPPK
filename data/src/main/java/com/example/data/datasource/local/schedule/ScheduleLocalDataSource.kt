package com.example.data.datasource.local.schedule

import com.example.data.model.local.ScheduleEntity // Local DB Entity 위치 가정
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * 일정 데이터의 로컬 데이터 소스 인터페이스입니다.
 * 주로 Room 데이터베이스와 상호작용합니다.
 */
interface ScheduleLocalDataSource {
    // 예시: 특정 날짜의 일정 가져오기 (Flow 사용)
    fun getSchedulesForDateStream(date: LocalDate): Flow<List<ScheduleEntity>>

    // 예시: 특정 월의 일정 요약 가져오기 (일정이 있는 날짜)
    suspend fun getDatesWithSchedulesForMonth(yearMonth: YearMonth): Set<LocalDate>

    // 예시: 일정 상세 정보 가져오기
    suspend fun getScheduleDetail(scheduleId: String): ScheduleEntity?

    // 예시: 일정 목록 저장 (기존 데이터 대체)
    suspend fun saveSchedules(schedules: List<ScheduleEntity>)

    // 예시: 단일 일정 추가 또는 업데이트 (Upsert)
    suspend fun upsertSchedule(schedule: ScheduleEntity)

    // 예시: 일정 삭제
    suspend fun deleteSchedule(scheduleId: String)

    // ... 향후 필요한 일정 관련 로컬 데이터 처리 함수 추가 ...
} 