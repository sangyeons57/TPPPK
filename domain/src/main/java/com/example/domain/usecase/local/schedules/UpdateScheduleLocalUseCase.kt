package com.example.domain.usecase.local.schedules

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.local.ScheduleLocalRepository
import javax.inject.Inject

interface UpdateScheduleLocalUseCase {
    suspend operator fun invoke(schedule: Schedule): CustomResult<Schedule, Exception>
}

class UpdateScheduleLocalUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : UpdateScheduleLocalUseCase {

    override suspend operator fun invoke(schedule: Schedule): CustomResult<Schedule, Exception> {
        return TODO("로컬 저장소에서 스케줄 정보를 업데이트")
    }
} 