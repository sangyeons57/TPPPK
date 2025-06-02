
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ScheduleDTO
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface ScheduleRemoteDataSource {

    /**
     * 특정 프로젝트의 특정 기간에 해당하는 모든 일정을 관찰합니다.
     * @param projectId 일정을 가져올 프로젝트의 ID
     * @param startAt 조회 시작 시간
     * @param endAt 조회 종료 시간
     */
    fun getSchedulesForProject(
        projectId: String,
        startAt: Timestamp,
        endAt: Timestamp
    ): Flow<List<ScheduleDTO>>

    /**
     * 특정 일정 하나의 상세 정보를 가져옵니다.
     * @param scheduleId 조회할 일정의 ID
     */
    suspend fun getSchedule(scheduleId: String): CustomResult<ScheduleDTO, Exception>

    /**
     * 새로운 일정을 생성합니다.
     * @param schedule 생성할 일정 정보 DTO
     * @return 생성된 일정의 ID를 포함한 Result 객체
     */
    suspend fun createSchedule(schedule: ScheduleDTO): CustomResult<String, Exception>

    /**
     * 기존 일정을 업데이트합니다.
     * @param schedule 새로운 정보를 담은 일정 DTO. documentId(`id`)가 반드시 포함되어야 합니다.
     */
    suspend fun updateSchedule(schedule: ScheduleDTO): CustomResult<Unit, Exception>

    /**
     * 일정을 삭제합니다.
     * @param scheduleId 삭제할 일정의 ID
     */
    suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception>


    /**
     * Firestore에서 지정된 년도와 월에 해당하는 모든 스케줄 DTO를 가져옵니다.
     *
     * @param yearMonth 가져올 스케줄의 년월 정보
     * @return 해당 월의 스케줄 DTO 목록을 담은 Flow
     */
    suspend fun getSchedulesForMonth(userId: String, yearMonth: YearMonth): Flow<CustomResult<List<ScheduleDTO>, Exception>>

    /**
     * Firestore에서 지정된 날짜에 시작하는 모든 스케줄 DTO를 가져옵니다.
     *
     * @param date 스케줄을 가져올 특정 날짜
     * @return 해당 날짜의 스케줄 DTO 목록을 담은 Flow
     */
    suspend fun getSchedulesOnDate(userId: String, date: LocalDate): Flow<CustomResult<List<ScheduleDTO>, Exception>>

    /**
     * Firestore에서 지정된 사용자의 특정 연월에 일정이 있는 날짜들의 요약 정보를 가져옵니다.
     *
     * @param userId 사용자 ID
     * @param yearMonth 요약 정보를 가져올 연월
     * @return 해당 월에 일정이 있는 날짜들의 Set. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    suspend fun getScheduleSummaryForMonth(userId: String, yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception>
}

