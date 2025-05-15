// 경로: data/repository/ScheduleRepositoryImpl.kt
package com.example.data.repository

import com.example.data.datasource.remote.schedule.ScheduleRemoteDataSource
import com.example.domain.model.Schedule
import com.example.domain.repository.ScheduleRepository
// import com.google.firebase.Timestamp // No longer directly constructing Timestamp here
import java.time.* // LocalDate, YearMonth, LocalTime 등 사용
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result // Result 클래스 사용
import com.example.core_common.util.DateTimeUtil // Import DateTimeUtil
import com.example.data.model.mapper.ScheduleMapper

/**
 * ScheduleRepository 인터페이스의 구현체입니다.
 * 원격 데이터 소스(ScheduleRemoteDataSource)와 상호작용하여 일정 데이터를 관리하고,
 * 결과를 도메인 모델로 변환하여 Result 객체로 래핑합니다.
 * Hilt를 통해 데이터 소스를 주입받습니다.
 */
@Singleton // 애플리케이션 전역에서 싱글톤으로 관리
class ScheduleRepositoryImpl @Inject constructor(
    private val remoteDataSource: ScheduleRemoteDataSource, // 원격 데이터 소스 주입
    private val scheduleMapper: ScheduleMapper // Inject ScheduleMapper
) : ScheduleRepository {

    // 시간 변환 시 UTC 시간대 사용
    private val zoneId = ZoneId.of("UTC")

    /**
     * 특정 날짜의 일정을 가져옵니다.
     * LocalDate를 해당 날짜의 시작과 끝 Timestamp로 변환하여 데이터 소스를 호출합니다.
     */
    override suspend fun getSchedulesForDate(date: LocalDate): Result<List<Schedule>> {
        return runCatching {
            // LocalDate를 UTC 기준 해당 날짜의 시작(00:00:00Z)과 끝(23:59:59.999Z) Timestamp로 변환
            val startInstant = date.atStartOfDay(zoneId).toInstant()
            val endInstant = date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
            val startOfDay = DateTimeUtil.instantToFirebaseTimestamp(startInstant)
            val endOfDay = DateTimeUtil.instantToFirebaseTimestamp(endInstant)

            // 데이터 소스 호출 및 DTO 리스트를 도메인 모델 리스트로 변환
            val dtoList = remoteDataSource.getSchedulesForDate(startOfDay!!, endOfDay!!)
            scheduleMapper.mapToDomainList(dtoList)
        }
        // runCatching은 성공 시 Result.success(value), 실패 시 Result.failure(exception) 반환
    }

    /**
     * 특정 월의 일정 요약 정보(일정이 있는 날짜 Set)를 가져옵니다.
     * YearMonth를 해당 월의 시작과 끝 Timestamp로 변환하여 데이터 소스를 호출하고,
     * 결과 DTO 리스트에서 날짜 정보만 추출하여 Set으로 만듭니다.
     */
    override suspend fun getScheduleSummaryForMonth(yearMonth: YearMonth): Result<Set<LocalDate>> {
        return runCatching {
            // YearMonth를 UTC 기준 해당 월의 시작일과 종료일의 Timestamp로 변환
            val startDayOfMonth = yearMonth.atDay(1)
            val endDayOfMonth = yearMonth.atEndOfMonth()
            val startInstant = startDayOfMonth.atStartOfDay(zoneId).toInstant()
            val endInstant = endDayOfMonth.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
            val startOfMonth = DateTimeUtil.instantToFirebaseTimestamp(startInstant)
            val endOfMonth = DateTimeUtil.instantToFirebaseTimestamp(endInstant)

            // 데이터 소스에서 해당 월의 모든 일정 DTO를 가져옴
            val schedulesDto = remoteDataSource.getSchedulesForMonth(startOfMonth!!, endOfMonth!!)

            // DTO 리스트에서 startTime을 LocalDate로 변환하고 중복 제거하여 Set 생성
            schedulesDto.mapNotNull { it.startTime } // startTime이 null인 경우 제외 (it.startTime is Firebase.Timestamp?)
                .mapNotNull { timestamp -> // Ensure timestamp is not null before conversion
                    // Timestamp -> Instant -> ZonedDateTime -> LocalDate
                    // LocalDate는 시간대 정보가 없으므로, UTC 기준으로 변환된 날짜를 사용
                    DateTimeUtil.firebaseTimestampToInstant(timestamp) // Changed
                        ?.atZone(zoneId)?.toLocalDate() // Apply toLocalDate after conversion
                }
                .toSet() // 중복 제거
        }
    }

    /**
     * 특정 ID의 일정 상세 정보를 가져옵니다.
     */
    override suspend fun getScheduleDetail(scheduleId: String): Result<Schedule> {
        return runCatching {
            // 데이터 소스 호출 및 DTO를 도메인 모델로 변환
            val dto = remoteDataSource.getScheduleDetail(scheduleId)
            scheduleMapper.mapToDomain(dto)
        }
    }

    /**
     * 새로운 일정을 추가합니다.
     * 도메인 모델을 DTO로 변환하여 데이터 소스를 호출합니다.
     */
    override suspend fun addSchedule(newScheduleData: Schedule): Result<Unit> {
        return runCatching {
            // 도메인 모델을 DTO로 변환 (id는 DTO 변환 시 제외됨)
            val scheduleDto = scheduleMapper.mapToDto(newScheduleData)
            // 데이터 소스 호출 (반환되는 ID는 사용하지 않음)
            remoteDataSource.addSchedule(scheduleDto)
            // 성공 시 Unit 반환 (runCatching이 처리)
        }
    }

    /**
     * 특정 ID의 일정을 삭제합니다.
     */
    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        return runCatching {
            // 데이터 소스 호출
            remoteDataSource.deleteSchedule(scheduleId)
            // 성공 시 Unit 반환 (runCatching이 처리)
        }
    }

    /**
     * 기존 일정을 업데이트합니다.
     * 도메인 모델을 DTO로 변환하여 데이터 소스를 호출합니다.
     */
    override suspend fun updateSchedule(updatedScheduleData: Schedule): Result<Unit> {
        return runCatching {
            // 업데이트 대상 문서 ID 확인
            val scheduleId = updatedScheduleData.id
            // 도메인 모델을 DTO로 변환 (id는 DTO 변환 시 제외됨)
            val scheduleDto = scheduleMapper.mapToDto(updatedScheduleData)
            // 데이터 소스 호출
            remoteDataSource.updateSchedule(scheduleId, scheduleDto)
            // 성공 시 Unit 반환 (runCatching이 처리)
        }
    }
}