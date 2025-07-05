package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ScheduleRepository
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    override val factoryContext: ScheduleRepositoryFactoryContext,
) : DefaultRepositoryImpl(scheduleRemoteDataSource, factoryContext), ScheduleRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Schedule)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type User"))
        ensureCollection()
        return if (entity.isNew) {
            scheduleRemoteDataSource.create(entity.toDto())
        } else {
            scheduleRemoteDataSource.update(entity.id,entity.getChangedFields())
        }
    }

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
                is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
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
                is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}
