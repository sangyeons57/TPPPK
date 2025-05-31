package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * 특정 일정의 상세 정보를 가져오는 유스케이스 인터페이스
 */
interface GetScheduleDetailUseCase {
    suspend operator fun invoke(scheduleId: String): CustomResult<Schedule, Exception>
}

/**
 * GetScheduleDetailUseCase의 구현체
 * @param scheduleRepository 일정 데이터 접근을 위한 Repository
 */
class GetScheduleDetailUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : GetScheduleDetailUseCase {

    /**
     * 유스케이스를 실행하여 특정 일정의 상세 정보를 가져옵니다.
     * @param scheduleId 조회할 일정의 ID
     * @return Result<Schedule> 일정 상세 정보 로드 결과
     */
    override suspend fun invoke(scheduleId: String): CustomResult<Schedule, Exception> {
        return scheduleRepository.getScheduleDetails(scheduleId)
    }
} 