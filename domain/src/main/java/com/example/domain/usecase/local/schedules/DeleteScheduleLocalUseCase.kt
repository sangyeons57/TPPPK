package com.example.domain.usecase.local.schedules

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ScheduleLocalRepository
import javax.inject.Inject

interface DeleteScheduleLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        scheduleId: DocumentId
    ): CustomResult<Unit, Exception>
}

class DeleteScheduleLocalUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : DeleteScheduleLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        scheduleId: DocumentId
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 특정 스케줄을 삭제")
    }
} 