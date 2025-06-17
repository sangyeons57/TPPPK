package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * 개인 및 프로젝트 스케줄 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ScheduleRepository {
    /**
     * Saves the [schedule] aggregate. If a document with the same id exists it will be merged (upsert).
     */
    suspend fun save(schedule: Schedule): CustomResult<String, Exception>
    /**
     * Retrieves a [Schedule] aggregate by id.
     */
    suspend fun findById(scheduleId: String): CustomResult<Schedule, Exception>

    /**
     * Observes the [Schedule] aggregate in real time.
     */
    fun observe(scheduleId: String): Flow<CustomResult<Schedule, Exception>>

    /**
     * Deletes the schedule document.
     */
    suspend fun delete(scheduleId: String): CustomResult<Unit, Exception>
    suspend fun findByDateSummaryForMonth(userId: String, yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception>

    /**
     * 지정된 년도와 월에 해당하는 모든 스케줄을 가져옵니다.
     * Firestore의 startTime을 기준으로 해당 월의 시작과 끝 사이의 스케줄을 조회합니다.
     *
     * @param yearMonth 가져올 스케줄의 년월 정보 (java.time.YearMonth)
     * @return 해당 월의 스케줄 목록을 담은 Flow. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    suspend fun findByMonth(userId: String, yearMonth: YearMonth): Flow<CustomResult<List<Schedule>, Exception>>

    /**
     * 지정된 날짜에 시작하는 모든 스케줄을 가져옵니다.
     *
     * @param date 스케줄을 가져올 특정 날짜 (java.time.LocalDate)
     * @return 해당 날짜의 스케줄 목록을 담은 Flow. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    fun findByDate(date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>>

    // 기존 String 타입의 getSchedulesForDate는 유지하거나,
    // 점진적으로 LocalDate 타입으로 마이그레이션 후 제거하는 것을 고려할 수 있습니다.
    fun findByDate(date: String): Flow<CustomResult<List<Schedule>, Exception>>
}
