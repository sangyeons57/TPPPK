package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * 특정 일정을 삭제하는 유스케이스 인터페이스
 */
interface DeleteScheduleUseCase {
    suspend operator fun invoke(scheduleId: String): CustomResult<Unit, Exception>
}

/**
 * DeleteScheduleUseCase의 구현체
 * @param scheduleRepository 일정 데이터 접근을 위한 Repository
 */
class DeleteScheduleUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository
) : DeleteScheduleUseCase {

    /**
     * 유스케이스를 실행하여 특정 일정을 삭제합니다.
     * @param scheduleId 삭제할 일정의 ID
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(scheduleId: String): CustomResult<Unit, Exception> {
        val session = authRepository.getCurrentUserSession()
        when (session) {
            is CustomResult.Success -> scheduleRepository.deleteSchedule(scheduleId)
            else -> CustomResult.Failure(Exception("로그인이 필요합니다."))
        }
    }
} 