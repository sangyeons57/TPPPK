package com.example.domain.usecase.local.schedules

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ScheduleLocalRepository
import java.time.LocalDate
import javax.inject.Inject

interface GetSchedulesForDateLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        date: LocalDate
    ): CustomResult<List<Schedule>, Exception>
}

class GetSchedulesForDateLocalUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : GetSchedulesForDateLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        date: LocalDate
    ): CustomResult<List<Schedule>, Exception> {
        return TODO("로컬 저장소에서 특정 날짜의 스케줄 목록 조회")
    }
} 