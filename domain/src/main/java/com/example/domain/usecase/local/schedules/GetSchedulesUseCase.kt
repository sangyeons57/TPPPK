package com.example.domain.usecase.local.schedules

import com.example.domain.model.Schedule
import com.example.domain.repository.local.ScheduleLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

interface GetSchedulesUseCase {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<Schedule>>
}

class GetSchedulesUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : GetSchedulesUseCase {

    override operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<Schedule>> {
        return TODO("특정 기간 사이의 모든 일정을 관찰")
    }
} 