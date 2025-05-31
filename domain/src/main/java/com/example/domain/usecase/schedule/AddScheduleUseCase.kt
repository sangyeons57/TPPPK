package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * 새로운 일정을 추가하는 유스케이스 인터페이스
 */
interface AddScheduleUseCase {
    suspend operator fun invoke(schedule: Schedule): CustomResult<Unit, Exception>
}

/**
 * AddScheduleUseCase의 구현체
 * @param scheduleRepository 일정 데이터 접근을 위한 Repository
 */
class AddScheduleUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : AddScheduleUseCase {

    /**
     * 유스케이스를 실행하여 새로운 일정을 추가합니다.
     * @param schedule 추가할 일정 정보
     * @return Result<Unit> 일정 추가 처리 결과
     */
    override suspend fun invoke(schedule: Schedule): CustomResult<Unit, Exception> {
        return scheduleRepository.createSchedule(schedule)
    }
} 