package com.example.domain.usecase.local.schedules

import com.example.domain.model.Schedule
import com.example.domain.repository.local.ScheduleLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetScheduleUseCase {
    operator fun invoke(scheduleId: String): Flow<Schedule?>
}

class GetScheduleUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : GetScheduleUseCase {

    override operator fun invoke(scheduleId: String): Flow<Schedule?> {
        return TODO("특정 일정 정보를 실시간으로 관찰")
    }
} 