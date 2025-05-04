package com.example.data.datasource.local.schedule

import com.example.data.db.dao.ScheduleDao // Room DAO 위치 가정
import com.example.data.model.local.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf // 예시 Flow 반환용
import java.time.LocalDate
import java.time.LocalDateTime // LocalDateTime 임포트 추가
import java.time.LocalTime   // LocalTime 임포트 추가
import java.time.YearMonth
import java.time.ZoneOffset  // ZoneOffset 임포트 추가 (UTC 변환 시 필요할 수 있음)
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScheduleLocalDataSource 인터페이스의 Room 데이터베이스 구현체입니다.
 */
@Singleton
class ScheduleLocalDataSourceImpl @Inject constructor(
    private val scheduleDao: ScheduleDao // Room DAO 주입
) : ScheduleLocalDataSource {

    // --- ScheduleLocalDataSource 인터페이스 함수 구현 --- 

    // 특정 날짜의 일정 가져오기 (Flow 사용)
    override fun getSchedulesForDateStream(date: LocalDate): Flow<List<ScheduleEntity>> {
        // TODO: LocalDateTime TypeConverter 구현 후 주석 해제 및 아래 임시 반환 제거
        /* 
        val startOfDay = date.atStartOfDay() // 또는 UTC 기준 필요 시 atStartOfDay(ZoneOffset.UTC)
        val endOfDay = date.atTime(LocalTime.MAX) // 또는 UTC 기준 필요 시 atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toLocalDateTime()
        return scheduleDao.getSchedulesForDateStream(startOfDay, endOfDay)
        */
        println("WARN: ScheduleLocalDataSourceImpl.getSchedulesForDateStream requires LocalDateTime TypeConverter. Returning empty flow.")
        return flowOf(emptyList()) // 임시 반환값 (TypeConverter 구현 전까지)
    }

    // 특정 월의 일정 요약 가져오기 (일정이 있는 날짜)
    override suspend fun getDatesWithSchedulesForMonth(yearMonth: YearMonth): Set<LocalDate> {
         // TODO: LocalDateTime TypeConverter 구현 후 주석 해제 및 아래 임시 반환 제거
        /*
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)
        // DAO는 LocalDateTime 리스트 반환 -> LocalDate로 변환 필요
        return scheduleDao.getDatesWithSchedulesBetween(startOfMonth, endOfMonth)
            .map { it.toLocalDate() } // 또는 date() 함수 결과가 String/Long 이면 그에 맞게 변환
            .toSet()
        */
        println("WARN: ScheduleLocalDataSourceImpl.getDatesWithSchedulesForMonth requires LocalDateTime TypeConverter. Returning empty set.")
        return emptySet() // 임시 반환값 (TypeConverter 구현 전까지)
    }

    // 일정 상세 정보 가져오기
    override suspend fun getScheduleDetail(scheduleId: String): ScheduleEntity? {
        return scheduleDao.getScheduleById(scheduleId)
    }

    // 일정 목록 저장 (기존 데이터 대체)
    override suspend fun saveSchedules(schedules: List<ScheduleEntity>) {
        // 기존 데이터 삭제 후 새 데이터 삽입 (Transaction 보장)
        scheduleDao.clearAndInsertSchedules(schedules)
    }

    // 단일 일정 추가 또는 업데이트 (Upsert)
    override suspend fun upsertSchedule(schedule: ScheduleEntity) {
        scheduleDao.upsertSchedule(schedule)
    }

    // 일정 삭제
    override suspend fun deleteSchedule(scheduleId: String) {
        scheduleDao.deleteScheduleById(scheduleId)
    }

    // ... 다른 함수들의 실제 구현 추가 ...
} 