package com.example.domain.usecase.local.schedules

import com.example.domain.model.Schedule
import com.example.domain.repository.local.ScheduleLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetSchedulesByProjectUseCase {
    operator fun invoke(projectId: String): Flow<List<Schedule>>
}

class GetSchedulesByProjectUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : GetSchedulesByProjectUseCase {

    override operator fun invoke(projectId: String): Flow<List<Schedule>> {
        return TODO("특정 프로젝트의 모든 일정을 실시간으로 관찰")
    }
} 