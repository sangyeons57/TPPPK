package com.example.domain.usecase.local.schedules

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ScheduleLocalRepository
import javax.inject.Inject

interface GetScheduleDetailLocalUseCase {
    suspend operator fun invoke(scheduleId: DocumentId): CustomResult<Schedule, Exception>
}

class GetScheduleDetailLocalUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : GetScheduleDetailLocalUseCase {

    override suspend operator fun invoke(scheduleId: DocumentId): CustomResult<Schedule, Exception> {
        return TODO("로컬 저장소에서 특정 스케줄의 상세 정보 조회")
    }
} 