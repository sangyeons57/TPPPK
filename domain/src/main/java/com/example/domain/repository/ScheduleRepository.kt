// 경로: domain/repository/ScheduleRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.Schedule
import java.time.LocalDate
import java.time.YearMonth
import kotlin.Result

/**
 * 일정 데이터에 접근하기 위한 Repository 인터페이스입니다.
 * 데이터 레이어(`:data` 모듈)에서 이 인터페이스를 구현하여 Firestore 등 실제 데이터 소스와의 상호작용을 처리합니다.
 * 모든 함수는 코루틴 컨텍스트에서 실행되어야 하므로 `suspend` 키워드를 사용합니다.
 * 작업의 성공 또는 실패(예외 포함)를 나타내기 위해 `kotlin.Result`를 반환 타입으로 사용합니다.
 */
interface ScheduleRepository {
    /**
     * 특정 날짜에 해당하는 모든 일정 목록을 비동기적으로 가져옵니다.
     *
     * @param date 조회하고자 하는 특정 날짜 (LocalDate 타입).
     * @return `Result<List<Schedule>>`: 성공 시 해당 날짜의 일정(`Schedule`) 리스트를 포함하는 Result. 실패 시 예외를 포함하는 Result.
     *         데이터 레이어 구현 시, 해당 날짜의 00:00:00부터 23:59:59.999 사이의 `startTime`을 가진 일정을 쿼리해야 합니다.
     */
    suspend fun getSchedulesForDate(date: LocalDate): Result<List<Schedule>>

    /**
     * 특정 연월에 일정이 하나 이상 존재하는 날짜들의 집합을 비동기적으로 가져옵니다.
     * 주로 캘린더 UI에서 해당 월의 어느 날짜에 일정이 있는지 시각적으로 표시(예: 점 표시)하기 위해 사용됩니다.
     *
     * @param yearMonth 조회하고자 하는 연월 (YearMonth 타입).
     * @return `Result<Set<LocalDate>>`: 성공 시 해당 월 내에 일정이 있는 날짜(`LocalDate`)들의 Set을 포함하는 Result. 실패 시 예외를 포함하는 Result.
     *         데이터 레이어 구현 시, 해당 월의 시작일과 종료일 사이의 `startTime`을 가진 일정을 쿼리하고, 그 날짜들을 추출해야 합니다.
     */
    suspend fun getScheduleSummaryForMonth(yearMonth: YearMonth): Result<Set<LocalDate>>

    /**
     * 제공된 ID에 해당하는 특정 일정의 상세 정보를 비동기적으로 가져옵니다.
     *
     * @param scheduleId 조회하고자 하는 일정의 고유 Firestore 문서 ID.
     * @return `Result<Schedule>`: 성공 시 해당 ID의 `Schedule` 객체를 포함하는 Result. 실패 시 (예: 문서를 찾을 수 없음) 예외를 포함하는 Result.
     */
    suspend fun getScheduleDetail(scheduleId: String): Result<Schedule>

    /**
     * 새로운 일정을 Firestore에 비동기적으로 추가합니다.
     *
     * @param newScheduleData 추가할 일정 정보를 담고 있는 `Schedule` 객체. 데이터 레이어에서 ID 필드는 무시하고 Firestore가 자동 생성하도록 처리할 수 있습니다.
     * @return `Result<Unit>`: 성공 시 `Result.success(Unit)`. 실패 시 예외를 포함하는 Result.
     */
    suspend fun addSchedule(newScheduleData: Schedule): Result<Unit>

    /**
     * 제공된 ID를 가진 일정을 Firestore에서 비동기적으로 삭제합니다.
     *
     * @param scheduleId 삭제하고자 하는 일정의 고유 Firestore 문서 ID.
     * @return `Result<Unit>`: 성공 시 `Result.success(Unit)`. 실패 시 예외를 포함하는 Result.
     */
    suspend fun deleteSchedule(scheduleId: String): Result<Unit>

    /**
     * 기존 일정을 Firestore에서 비동기적으로 업데이트합니다.
     *
     * @param updatedScheduleData 업데이트할 내용을 포함하는 `Schedule` 객체. `id` 필드를 사용하여 대상 문서를 식별해야 합니다.
     * @return `Result<Unit>`: 성공 시 `Result.success(Unit)`. 실패 시 예외를 포함하는 Result.
     */
    suspend fun updateSchedule(updatedScheduleData: Schedule): Result<Unit>
}