package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.base.ScheduleRepository
import javax.inject.Inject

interface UpdateScheduleUseCase {
    suspend operator fun invoke(schedule: Schedule): CustomResult<String, Exception>
}
class UpdateScheduleUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : UpdateScheduleUseCase {
    override suspend operator fun invoke(schedule: Schedule): CustomResult<String, Exception> {
        // Basic validation, can be expanded
        if (schedule.title.value.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Title cannot be blank."))
        }
        if (schedule.endTime.isBefore(schedule.startTime)) {
            return CustomResult.Failure(IllegalArgumentException("End time must be after start time."))
        }

        return scheduleRepository.save(schedule)
    }
}

