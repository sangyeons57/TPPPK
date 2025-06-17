package com.example.domain.usecase.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 특정 월의 일정 요약 정보를 가져오는 유스케이스입니다.
 * 일정 존재 여부를 날짜별로 반환합니다.
 *
 * @property scheduleRepository ScheduleRepository 인터페이스의 구현체
 */
class GetScheduleSummaryForMonthUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 연월에 해당하는 일정 요약 정보(일정이 있는 날짜 세트)를 가져옵니다.
     *
     * @param yearMonth 정보를 가져올 연월
     * @return Result 객체. 성공 시 일정이 있는 날짜들의 Set<LocalDate>를, 실패 시 예외를 포함합니다.
     */
    suspend operator fun invoke(yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception> {
        val userSession =  authRepository.getCurrentUserSession()
        return when (userSession) {
            is CustomResult.Success -> scheduleRepository.findByDateSummaryForMonth(userSession.data.userId, yearMonth)
            else -> CustomResult.Failure(Exception("로그인이 필요합니다."))
        }
    }
} 