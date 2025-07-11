package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

/**
 * 특정 날짜에 해당하는 일정 목록을 가져오는 유스케이스 인터페이스
 */
interface GetSchedulesForDateUseCase {
    suspend operator fun invoke(date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>>
}

/**
 * GetSchedulesForDateUseCase의 구현체
 * @param scheduleRepository 일정 데이터 접근을 위한 Repository
 */
class GetSchedulesForDateUseCaseImpl @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository,
) : GetSchedulesForDateUseCase {

    /**
     * 유스케이스를 실행하여 특정 날짜의 일정 목록을 가져옵니다.
     * @param date 조회할 날짜
     * @return Result<List<Schedule>> 일정 목록 로드 결과
     */
    override suspend fun invoke(date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>> {
        val userSession = when (val result = authRepository.getCurrentUserSession() ){
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> return flowOf(CustomResult.Failure(result.error))
            is CustomResult.Initial -> return flowOf(CustomResult.Initial)
            is CustomResult.Loading -> return flowOf(CustomResult.Loading)
            is CustomResult.Progress -> return flowOf(CustomResult.Progress(result.progress))
        }

        return scheduleRepository.findByDate(userSession.userId, date)
    }
} 