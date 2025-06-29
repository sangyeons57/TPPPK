package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
/**
 * 개인 및 프로젝트 스케줄 관련 데이터 처리를 위한 인터페이스입니다.
 */
import com.example.domain.model.vo.DocumentId // Ensure DocumentId is imported
import com.example.domain.model.vo.UserId

interface ScheduleRepository : DefaultRepository {
    override val factoryContext: ScheduleRepositoryFactoryContext

    suspend fun findByDateSummaryForMonth(userId: UserId, yearMonth: YearMonth): CustomResult<Set<LocalDate>, Exception>

    /**
     * 지정된 년도와 월에 해당하는 모든 스케줄을 가져옵니다.
     * Firestore의 startTime을 기준으로 해당 월의 시작과 끝 사이의 스케줄을 조회합니다.
     *
     * @param yearMonth 가져올 스케줄의 년월 정보 (java.time.YearMonth)
     * @return 해당 월의 스케줄 목록을 담은 Flow. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    suspend fun findByMonth(userId: UserId, yearMonth: YearMonth): Flow<CustomResult<List<Schedule>, Exception>> // Changed to non-suspend

    /**
     * 지정된 날짜에 시작하는 모든 스케줄을 가져옵니다.
     *
     * @param date 스케줄을 가져올 특정 날짜 (java.time.LocalDate)
     * @return 해당 날짜의 스케줄 목록을 담은 Flow. CustomResult로 성공 또는 실패를 나타냅니다.
     */
    suspend fun findByDate(userId: UserId, date: LocalDate): Flow<CustomResult<List<Schedule>, Exception>> // Added userId
}
