package com.example.domain.usecase.schedule

import com.example.domain.model.Schedule
import com.example.domain.repository.ScheduleRepository
import javax.inject.Inject

interface UpdateScheduleUseCase {
    suspend operator fun invoke(schedule: Schedule): Result<Unit>
}
class UpdateScheduleUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : UpdateScheduleUseCase {
    override suspend operator fun invoke(schedule: Schedule): Result<Unit> {
        // Basic validation, can be expanded
        if (schedule.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title cannot be blank."))
        }
        if (schedule.endTime.isBefore(schedule.startTime)) {
            return Result.failure(IllegalArgumentException("End time must be after start time."))
        }
        return scheduleRepository.updateSchedule(schedule)
    }
}

