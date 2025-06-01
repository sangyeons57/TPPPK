package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.ScheduleRepository
import javax.inject.Inject

interface UpdateScheduleUseCase {
    suspend operator fun invoke(schedule: Schedule): CustomResult<Unit, Exception>
}
class UpdateScheduleUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : UpdateScheduleUseCase {
    override suspend operator fun invoke(schedule: Schedule): CustomResult<Unit, Exception> {
        // Basic validation, can be expanded
        if (schedule.title.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Title cannot be blank."))
        }
        if (schedule.endTime?.isBefore(schedule.startTime) == true) {
            return CustomResult.Failure(IllegalArgumentException("End time must be after start time."))
        }
        return scheduleRepository.updateSchedule(schedule)
    }
}

