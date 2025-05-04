package com.example.data.repository

import com.example.domain.model.Schedule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

/**
 * ScheduleRepository 기능 테스트
 *
 * 이 테스트는 FakeScheduleRepository를 사용하여 ScheduleRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class ScheduleRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var scheduleRepository: FakeScheduleRepository
    
    // 테스트 데이터
    private val testScheduleId = "test-schedule-123"
    private val testProjectId = "test-project-456"
    private val today = LocalDate.now()
    private val zoneId = ZoneId.systemDefault()

    private val testSchedule = Schedule(
        id = testScheduleId,
        projectId = testProjectId,
        title = "테스트 일정",
        content = "테스트 일정 내용",
        startTime = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 10, 0),
        endTime = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 12, 0),
        participants = listOf("user-1", "user-2"),
        isAllDay = false
    )
    
    // 추가 테스트 데이터 (다중 일정 테스트용)
    private val nextDaySchedule = testSchedule.copy(
        id = "test-schedule-124",
        startTime = LocalDateTime.of(today.plusDays(1).year, today.plusDays(1).month, today.plusDays(1).dayOfMonth, 14, 0),
        endTime = LocalDateTime.of(today.plusDays(1).year, today.plusDays(1).month, today.plusDays(1).dayOfMonth, 16, 0)
    )
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeScheduleRepository 초기화
        scheduleRepository = FakeScheduleRepository()
        
        // 테스트 일정 추가
        scheduleRepository.addScheduleData(testSchedule)
    }
    
    /**
     * 특정 날짜의 일정 목록 조회 테스트
     */
    @Test
    fun `getSchedulesForDate should return schedules for specific date`() = runBlocking {
        // When: 오늘 날짜의 일정 조회
        val result = scheduleRepository.getSchedulesForDate(today)
        
        // Then: 성공 및 올바른 일정 목록 반환
        assertTrue(result.isSuccess)
        val schedules = result.getOrNull()
        assertNotNull(schedules)
        assertEquals(1, schedules?.size)
        assertEquals(testSchedule, schedules?.firstOrNull())
    }
    
    /**
     * 다중 일정이 있는 날짜 조회 테스트
     */
    @Test
    fun `getSchedulesForDate should return multiple schedules when available`() = runBlocking {
        // Given: 추가 일정 데이터
        scheduleRepository.addScheduleData(
            testSchedule.copy(
                id = "test-schedule-125",
                title = "다른 테스트 일정"
            )
        )
        
        // When: 오늘 날짜의 일정 조회
        val result = scheduleRepository.getSchedulesForDate(today)
        
        // Then: 성공 및 두 개의 일정 반환
        assertTrue(result.isSuccess)
        val schedules = result.getOrNull()
        assertNotNull(schedules)
        assertEquals(2, schedules?.size)
    }
    
    /**
     * 날짜 범위에 걸친 일정 조회 테스트
     */
    @Test
    fun `getSchedulesForDate should handle multi-day schedules`() = runBlocking {
        // Given: 여러 날짜에 걸친 일정
        val multiDaySchedule = testSchedule.copy(
            id = "multi-day-schedule",
            startTime = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 10, 0),
            endTime = LocalDateTime.of(today.plusDays(2).year, today.plusDays(2).month, today.plusDays(2).dayOfMonth, 12, 0)
        )
        scheduleRepository.addScheduleData(multiDaySchedule)
        
        // When: 다음 날의 일정 조회
        val result = scheduleRepository.getSchedulesForDate(today.plusDays(1))
        
        // Then: 성공 및 해당 날짜에 걸친 일정 포함
        assertTrue(result.isSuccess)
        val schedules = result.getOrNull()
        assertNotNull(schedules)
        assertTrue(schedules?.any { it.id == "multi-day-schedule" } == true)
    }
    
    /**
     * 월별 일정 요약 조회 테스트
     */
    @Test
    fun `getScheduleSummaryForMonth should return dates with schedules`() = runBlocking {
        // Given: 다른 날짜의 일정 추가
        scheduleRepository.addScheduleData(nextDaySchedule)
        
        // When: 이번 달 일정 요약 조회
        val currentMonth = YearMonth.of(today.year, today.month)
        val result = scheduleRepository.getScheduleSummaryForMonth(currentMonth)
        
        // Then: 성공 및 일정이 있는 날짜들 반환
        assertTrue(result.isSuccess)
        val dates = result.getOrNull()
        assertNotNull(dates)
        assertEquals(2, dates?.size) // 오늘과 다음 날
        assertTrue(dates?.contains(today) == true)
        assertTrue(dates?.contains(today.plusDays(1)) == true)
    }
    
    /**
     * 일정 상세 조회 테스트
     */
    @Test
    fun `getScheduleDetail should return schedule details`() = runBlocking {
        // When: 일정 상세 조회
        val result = scheduleRepository.getScheduleDetail(testScheduleId)
        
        // Then: 성공 및 올바른 일정 정보 반환
        assertTrue(result.isSuccess)
        assertEquals(testSchedule, result.getOrNull())
    }
    
    /**
     * 존재하지 않는 일정 조회 시 실패 테스트
     */
    @Test
    fun `getScheduleDetail should fail for non-existent schedule`() = runBlocking {
        // When: 존재하지 않는 일정 상세 조회
        val result = scheduleRepository.getScheduleDetail("non-existent-id")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 일정 추가 테스트
     */
    @Test
    fun `addSchedule should add a new schedule`() = runBlocking {
        // Given: 새로운 일정
        val newSchedule = testSchedule.copy(
            id = "new-schedule-id",
            title = "새 일정"
        )
        
        // When: 일정 추가
        val result = scheduleRepository.addSchedule(newSchedule)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 추가된 일정 조회 확인
        val detailResult = scheduleRepository.getScheduleDetail("new-schedule-id")
        assertTrue(detailResult.isSuccess)
        assertEquals(newSchedule, detailResult.getOrNull())
    }
    
    /**
     * 유효하지 않은 일정 추가 실패 테스트 (빈 제목)
     */
    @Test
    fun `addSchedule should fail with blank title`() = runBlocking {
        // Given: 제목이 없는 일정
        val invalidSchedule = testSchedule.copy(
            id = "invalid-schedule-id",
            title = "   "
        )
        
        // When: 일정 추가
        val result = scheduleRepository.addSchedule(invalidSchedule)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("Schedule title cannot be blank", exception?.message)
    }
    
    /**
     * 유효하지 않은 일정 추가 실패 테스트 (종료 시간이 시작 시간보다 이전)
     */
    @Test
    fun `addSchedule should fail when end time is before start time`() = runBlocking {
        // Given: 종료 시간이 시작 시간보다 이전인 일정
        val invalidSchedule = testSchedule.copy(
            id = "invalid-schedule-id",
            startTime = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 14, 0),
            endTime = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 12, 0)
        )
        
        // When: 일정 추가
        val result = scheduleRepository.addSchedule(invalidSchedule)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertEquals("End time cannot be before start time", exception?.message)
    }
    
    /**
     * 일정 삭제 테스트
     */
    @Test
    fun `deleteSchedule should remove a schedule`() = runBlocking {
        // When: 일정 삭제
        val result = scheduleRepository.deleteSchedule(testScheduleId)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 삭제된 일정 조회 시 실패 확인
        val detailResult = scheduleRepository.getScheduleDetail(testScheduleId)
        assertTrue(detailResult.isFailure)
        assertTrue(detailResult.exceptionOrNull() is NoSuchElementException)
    }
    
    /**
     * 존재하지 않는 일정 삭제 실패 테스트
     */
    @Test
    fun `deleteSchedule should fail for non-existent schedule`() = runBlocking {
        // When: 존재하지 않는 일정 삭제
        val result = scheduleRepository.deleteSchedule("non-existent-id")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 일정 업데이트 테스트
     */
    @Test
    fun `updateSchedule should update an existing schedule`() = runBlocking {
        // Given: 업데이트할 일정 정보
        val updatedSchedule = testSchedule.copy(
            title = "업데이트된 일정",
            content = "업데이트된 내용"
        )
        
        // When: 일정 업데이트
        val result = scheduleRepository.updateSchedule(updatedSchedule)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // Then: 업데이트된 일정 조회 확인
        val detailResult = scheduleRepository.getScheduleDetail(testScheduleId)
        assertTrue(detailResult.isSuccess)
        assertEquals(updatedSchedule, detailResult.getOrNull())
    }
    
    /**
     * 존재하지 않는 일정 업데이트 실패 테스트
     */
    @Test
    fun `updateSchedule should fail for non-existent schedule`() = runBlocking {
        // Given: 존재하지 않는 일정 ID
        val nonExistentSchedule = testSchedule.copy(
            id = "non-existent-id"
        )
        
        // When: 일정 업데이트
        val result = scheduleRepository.updateSchedule(nonExistentSchedule)
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("Test error")
        scheduleRepository.setShouldSimulateError(true, testError)
        
        // When: 작업 수행
        val result = scheduleRepository.getSchedulesForDate(today)
        
        // Then: 실패 및 시뮬레이션된 에러 반환
        assertTrue(result.isFailure)
        assertEquals(testError, result.exceptionOrNull())
    }
} 