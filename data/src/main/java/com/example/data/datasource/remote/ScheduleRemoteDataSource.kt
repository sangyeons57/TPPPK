
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ScheduleDTO
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

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
}

