package com.example.domain.usecase.local.schedules

import com.example.domain.repository.local.ScheduleLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface DeleteScheduleUseCase {
    suspend operator fun invoke(scheduleId: String): CustomResult<Unit, Exception>
}

class DeleteScheduleUseCaseImpl @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) : DeleteScheduleUseCase {

    override suspend operator fun invoke(scheduleId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에서 특정 일정을 삭제")
    }
} 