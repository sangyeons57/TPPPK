package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.ScheduleRemoteDataSource
import com.example.data_model.remote.ScheduleDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Schedule
import com.example.domain.vo.UserId
import com.example.domain_repository.base.ScheduleRepository
import com.example.mapper.DtoMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    private val scheduleMapper: DtoMapper<Schedule, ScheduleDTO>,
) : DefaultRepositoryImpl<Schedule, ScheduleDTO>(scheduleRemoteDataSource, scheduleMapper),
    ScheduleRepository {

    override suspend fun findByDateSummaryForMonth(
        userId: UserId,
        yearMonth: YearMonth
    ): CustomResult<Set<LocalDate>, Exception> {
        ensureCollection()
        return scheduleRemoteDataSource.findDateSummaryForMonth(userId.value, yearMonth)
    }

    override suspend fun findByMonth(userId: UserId, yearMonth: YearMonth): Flow<CustomResult<List<Schedule>, Exception>> {
        ensureCollection()
        return scheduleRemoteDataSource.findByMonth(userId.value, yearMonth).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map {
                    mapper.dtoToDomain(it)
                })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override suspend fun findByDate(userId: UserId, date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>> {
        ensureCollection()
        return scheduleRemoteDataSource.findByDate(userId.value, date).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map {
                    mapper.dtoToDomain(it)
                })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}
