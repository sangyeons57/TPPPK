package com.example.domain.usecase.local.schedules

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ScheduleLocalRepository
import java.time.LocalDate
import javax.inject.Inject

interface GetScheduleSummaryForMonthLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        year: Int,
        month: Int
    ): CustomResult<Map<LocalDate, List<Schedule>>, Exception>
}

class GetScheduleSummaryForMonthLocalUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : GetScheduleSummaryForMonthLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        year: Int,
        month: Int
    ): CustomResult<Map<LocalDate, List<Schedule>>, Exception> {
        return TODO("로컬 저장소에서 특정 월의 스케줄 요약 정보 조회")
    }
} 