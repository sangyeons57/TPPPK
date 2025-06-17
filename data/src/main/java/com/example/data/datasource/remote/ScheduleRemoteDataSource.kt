
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ScheduleDTO
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface ScheduleRemoteDataSource {

    /**
     * 특정 일정 하나의 상세 정보를 가져옵니다.
     * @param scheduleId 조회할 일정의 ID
     */
    suspend fun findById(scheduleId: String): CustomResult<ScheduleDTO, Exception>

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
    suspend fun findByMonth(userId: String, yearMonth: YearMonth): Flow<CustomResult<List<ScheduleDTO>, Exception>>

    /**
     * Firestore에서 지정된 날짜에 시작하는 모든 스케줄 DTO를 가져옵니다.
     *
     * @param date 스케줄을 가져올 특정 날짜
     * @return 해당 날짜의 스케줄 DTO 목록을 담은 Flow
     */
    suspend fun findByDate(userId: String, date: LocalDate): Flow<CustomResult<List<ScheduleDTO>, Exception>>

    /**
     * Firestore에서 지정된 사용자의 특정 연월에 일정이 있는 날짜들의 요약 정보를 가져옵니다.
     *
     * @param userId 사용자 ID
     * @param yearMonth 요약 정보를 가져올 연월
     * @return 해당 월에 일정이 있는 날짜들의 Set. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    suspend fun findDateSummaryForMonth(userId: String, yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception>

    /**
     * 실시간으로 스케줄 문서를 관찰합니다.
     */
    fun observeSchedule(scheduleId: String): Flow<CustomResult<ScheduleDTO, Exception>>

    /**
     * 스케줄을 저장합니다. id가 비어있으면 새 문서를 만들고, 존재하면 병합 업데이트합니다.
     * @return 저장 후 문서 ID
     */
    suspend fun saveSchedule(schedule: ScheduleDTO): CustomResult<String, Exception>
}

