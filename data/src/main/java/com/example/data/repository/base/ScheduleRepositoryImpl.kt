package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.data.model.remote.ScheduleDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.User
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactoryContext
import com.example.domain.repository.base.ScheduleRepository
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    override val factoryContext: ScheduleRepositoryFactoryContext,
) : DefaultRepositoryImpl(scheduleRemoteDataSource, factoryContext.collectionPath), ScheduleRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Schedule)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type User"))

        return if (entity.id.isAssigned()) {
            scheduleRemoteDataSource.update(entity.id,entity.getChangedFields())
        } else {
            scheduleRemoteDataSource.create(entity.toDto())
        }
    }


    override suspend fun findByDateSummaryForMonth(
        userId: String,
        yearMonth: YearMonth
    ): CustomResult<Set<LocalDate>, Exception> {
        return scheduleRemoteDataSource.findDateSummaryForMonth(userId, yearMonth)
    }

    override suspend fun findByMonth(userId: String, yearMonth: YearMonth): Flow<CustomResult<List<Schedule>, Exception>> {
        return scheduleRemoteDataSource.findByMonth(userId, yearMonth).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override suspend fun findByDate(userId: String, date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>> {
        return scheduleRemoteDataSource.findByDate(userId, date).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override suspend fun findByDate(userId: String, date: String): Flow<CustomResult<List<Schedule>, Exception>> {
        return try {
            val localDate = LocalDate.parse(date)
            findByDate(userId, localDate)
        } catch (e: DateTimeParseException) {
            flowOf(CustomResult.Failure(IllegalArgumentException("Invalid date format: '$date'. Expected yyyy-MM-dd.", e)))
        } catch (e: Exception) {
            flowOf(CustomResult.Failure(e))
        }
    }
}
