package com.example.data.repository

import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.data.model.mapper.toDomain // ScheduleDTO -> Schedule
import com.example.data.model.mapper.toDto // Schedule -> ScheduleDTO
import com.example.domain.model.Schedule
import com.example.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource
    // private val scheduleMapper: ScheduleMapper // 개별 매퍼 사용시
) : ScheduleRepository {

    override suspend fun createSchedule(schedule: Schedule): Result<Schedule> = resultTry {
        val scheduleDto = schedule.toDto() // ID는 비어있을 수 있음
        // ScheduleRemoteDataSource의 createSchedule 함수는 생성된 DTO (ID 포함)를 반환한다고 가정
        scheduleRemoteDataSource.createSchedule(scheduleDto).getOrThrow().toDomain()
    }

    override suspend fun getScheduleDetails(scheduleId: String): Result<Schedule> = resultTry {
        scheduleRemoteDataSource.getSchedule(scheduleId).getOrThrow().toDomain()
    }

    override fun getUserSchedulesStream(userId: String, startDateMillis: Long, endDateMillis: Long): Flow<Result<List<Schedule>>> {
        // ScheduleRemoteDataSource에 getUserSchedulesStream(userId, startDate, endDate) 함수 필요
        return scheduleRemoteDataSource.getUserSchedulesStream(userId, startDateMillis, endDateMillis).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override fun getProjectSchedulesStream(projectId: String, startDateMillis: Long, endDateMillis: Long): Flow<Result<List<Schedule>>> {
        // ScheduleRemoteDataSource에 getProjectSchedulesStream(projectId, startDate, endDate) 함수 필요
        return scheduleRemoteDataSource.getProjectSchedulesStream(projectId, startDateMillis, endDateMillis).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun updateSchedule(schedule: Schedule): Result<Unit> = resultTry {
        val scheduleDto = schedule.toDto() // ID가 반드시 포함되어야 함
        scheduleRemoteDataSource.updateSchedule(scheduleDto).getOrThrow()
    }

    override suspend fun deleteSchedule(scheduleId: String, currentUserId: String): Result<Unit> = resultTry {
        // ScheduleRemoteDataSource에 deleteSchedule(scheduleId, currentUserId) 함수 필요 (권한 확인용)
        scheduleRemoteDataSource.deleteSchedule(scheduleId, currentUserId).getOrThrow()
    }

    override suspend fun getScheduleSummaryForMonth(userId: String, year: Int, month: Int): Result<Map<Int, Boolean>> = resultTry {
        // ScheduleRemoteDataSource에 getScheduleSummaryForMonth(userId, year, month) 함수 필요
        // 이 함수는 해당 월의 날짜별 스케줄 유무를 Map 형태로 반환한다고 가정
        scheduleRemoteDataSource.getScheduleSummaryForMonth(userId, year, month).getOrThrow()
    }
}
