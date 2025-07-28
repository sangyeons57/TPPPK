package com.example.domain.usecase.local.schedules

import com.example.domain.model.Schedule
import com.example.domain.repository.local.ScheduleLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface InsertScheduleUseCase {
    suspend operator fun invoke(schedule: Schedule): CustomResult<Unit, Exception>
}

class InsertScheduleUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : InsertScheduleUseCase {

    override suspend operator fun invoke(schedule: Schedule): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에 일정을 삽입하거나 갱신")
    }
} 