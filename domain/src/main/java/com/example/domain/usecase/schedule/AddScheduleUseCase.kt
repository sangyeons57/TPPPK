package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ScheduleRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 새로운 일정을 추가하는 유스케이스
 */
interface AddScheduleUseCase {
    suspend operator fun invoke(
        title: String,
        content: String,
        startTime: Instant,
        endTime: Instant,
        status: ScheduleStatus,
        color: String?,
        projectId: String?
    ): CustomResult<String, Exception>
}

class AddScheduleUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository
) : AddScheduleUseCase {
    override suspend operator fun invoke(
        title: String,
        content: String,
        startTime: Instant,
        endTime: Instant,
        status: ScheduleStatus,
        color: String?,
        projectId: String?
    ): CustomResult<String, Exception> {
        val session = when (val result =authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }

        // The ID will be generated by Firestore, so we pass an empty string for now.
        val newSchedule = Schedule.create(
            projectId = ProjectId(projectId ?:""),
            creatorId = OwnerId.from(session.userId),
            title = ScheduleTitle(title),
            content = ScheduleContent(content),
            startTime = startTime,
            endTime = endTime,
            status = status,
        )

        return when (val result = scheduleRepository.save(newSchedule)) {
            is CustomResult.Success -> CustomResult.Success(result.data.value)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}