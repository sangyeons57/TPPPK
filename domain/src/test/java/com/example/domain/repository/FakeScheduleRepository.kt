package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ScheduleRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class FakeScheduleRepository : ScheduleRepository {

    private val schedules = mutableMapOf<String, Schedule>()
    private var shouldThrowError = false

    override val factoryContext: DefaultRepositoryFactoryContext
        get() = TODO("Not yet implemented")

    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }

    fun addSchedule(schedule: Schedule) {
        schedules[schedule.id.value] = schedule
    }

    override suspend fun findById(
        id: DocumentId,
        source: Source
    ): CustomResult<AggregateRoot, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find by id failed"))
        }
        return schedules[id.value]?.let {
            CustomResult.Success(it)
        } ?: CustomResult.Failure(Exception("Schedule not found"))
    }

    override suspend fun create(
        id: DocumentId,
        entity: AggregateRoot
    ): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Create failed"))
        }
        val schedule = entity as Schedule
        schedules[id.value] = schedule
        return CustomResult.Success(id)
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Save failed"))
        }
        val schedule = entity as Schedule
        schedules[schedule.id.value] = schedule
        return CustomResult.Success(schedule.id)
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Delete failed"))
        }
        return if (schedules.remove(id.value) != null) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(Exception("Schedule not found"))
        }
    }

    override suspend fun findAll(source: Source): CustomResult<List<AggregateRoot>, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Find all failed"))
        }
        return CustomResult.Success(schedules.values.toList())
    }

    override fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe failed")))
        }
        return flowOf(
            schedules[id.value]?.let { CustomResult.Success(it) }
                ?: CustomResult.Failure(Exception("Schedule not found"))
        )
    }

    override fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>> {
        if (shouldThrowError) {
            return flowOf(CustomResult.Failure(Exception("Observe all failed")))
        }
        return flowOf(CustomResult.Success(schedules.values.toList()))
    }

    // New repository API stubs
    override suspend fun findByDateSummaryForMonth(
        userId: UserId,
        yearMonth: java.time.YearMonth
    ): CustomResult<Set<LocalDate>, Exception> {
        return CustomResult.Success(emptySet())
    }

    override suspend fun findByMonth(
        userId: UserId,
        yearMonth: java.time.YearMonth
    ): Flow<CustomResult<List<Schedule>, Exception>> {
        return flowOf(CustomResult.Success(emptyList()))
    }

    override suspend fun findByDate(
        userId: UserId,
        date: LocalDate
    ): Flow<CustomResult<List<Schedule>, Exception>> {
        return flowOf(CustomResult.Success(emptyList()))
    }
}