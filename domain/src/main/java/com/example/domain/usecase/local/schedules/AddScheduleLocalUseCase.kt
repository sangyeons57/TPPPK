package com.example.domain.usecase.local.schedules

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.local.ScheduleLocalRepository
import javax.inject.Inject

interface AddScheduleLocalUseCase {
    suspend operator fun invoke(schedule: Schedule): CustomResult<Schedule, Exception>
}

class AddScheduleLocalUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : AddScheduleLocalUseCase {

    override suspend operator fun invoke(schedule: Schedule): CustomResult<Schedule, Exception> {
        return TODO("로컬 저장소에 새 스케줄을 추가")
    }
} 