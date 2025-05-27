package com.example.data.repository

import com.google.firebase.Timestamp
import com.example.domain.model.Schedule
import com.example.data.datasource.remote.schedule.ScheduleRemoteDataSource
import com.example.data.model.remote.schedule.ScheduleDto
import com.example.data.model.mapper.toDomain
import com.example.data.model.mapper.toDto
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

/**
 * ScheduleRepositoryImpl의 단위 테스트 클래스.
 * MockK를 사용하여 의존성을 모의 객체로 대체하고 Repository의 로직을 검증합니다.
 */
class ScheduleRepositoryImplTest {

    // 모의(Mock) 객체 생성
    @MockK
    private lateinit var mockDataSource: ScheduleRemoteDataSource
    
    // 테스트 대상 객체
    private lateinit var scheduleRepository: ScheduleRepositoryImpl

    // Use UTC for testing, consistent with the implementation
    private val zoneId: ZoneId = ZoneId.of("UTC")

    // --- 테스트 데이터 및 상수 ---
    private val testUserId = "testUser123"
    private val testProjectId = "projectABC"
    private val testYearMonth = YearMonth.of(2024, 7)
    private val testDate = LocalDate.of(2024, 7, 20)
    private val testLocalDateTime = LocalDateTime.of(testDate, LocalTime.NOON) // 2024-07-20T12:00
    private val testTimestamp = Timestamp(java.util.Date.from(testLocalDateTime.atZone(zoneId).toInstant()))
    private val date1 = testYearMonth.atDay(10)
    private val date2 = testYearMonth.atDay(20)

    // Helper functions using the repository's zoneId
    private fun getStartOfMonthTimestamp(yearMonth: YearMonth): Timestamp {
        return Timestamp(Date.from(yearMonth.atDay(1).atStartOfDay(zoneId).toInstant()))
    }

    private fun getEndOfMonthTimestamp(yearMonth: YearMonth): Timestamp {
        return Timestamp(Date.from(yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zoneId).toInstant()))
    }

    private fun getStartOfDayTimestamp(date: LocalDate): Timestamp {
        return Timestamp(Date.from(date.atStartOfDay(zoneId).toInstant()))
    }

    private fun getEndOfDayTimestamp(date: LocalDate): Timestamp {
        return Timestamp(Date.from(date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()))
    }

    // Test data 
    private val testScheduleDto = ScheduleDto(
        id = "schedule1",
        projectId = testProjectId,
        title = "Test Event DTO",
        content = "DTO Content",
        startTime = testTimestamp,
        endTime = testTimestamp,
        participants = listOf("user1", "user2"),
        isAllDay = false
    )

    // Aligned with Schedule.kt definition
    private val testScheduleDomain = Schedule(
        id = "schedule1",
        projectId = testProjectId,
        title = "Test Event Domain",
        content = "Domain Content",
        startTime = testLocalDateTime,
        endTime = testLocalDateTime,
        participants = listOf("user1", "user2"),
        isAllDay = false
    )

    // 테스트 실행 전 MockK 초기화
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic("com.example.data.model.mapper.ScheduleMapperKt")
        
        // Create the repository with the mocked data source
        scheduleRepository = ScheduleRepositoryImpl(mockDataSource)
        
        // Default mocking behavior
        every { testScheduleDomain.toDto() } returns testScheduleDto
        every { testScheduleDto.toDomain() } returns testScheduleDomain
        every { listOf(testScheduleDto).toDomain() } returns listOf(testScheduleDomain)
    }

    /**
     * Repository.getSchedulesForDate 테스트 (성공 케이스)
     */
    @Test
    fun `getSchedulesForDate returns success`() = runTest {
        // Arrange
        val startOfDay = getStartOfDayTimestamp(testDate)
        val endOfDay = getEndOfDayTimestamp(testDate)
        val dtoList = listOf(testScheduleDto)
        val domainList = listOf(testScheduleDomain)

        coEvery { mockDataSource.getSchedulesForDate(startOfDay, endOfDay) } returns dtoList
        
        // Mock the extension function for this specific test
        every { dtoList.toDomain() } returns domainList

        // Act
        val result = scheduleRepository.getSchedulesForDate(testDate)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(domainList, result.getOrNull())
        coVerify { mockDataSource.getSchedulesForDate(startOfDay, endOfDay) }
        verify { dtoList.toDomain() }
    }

    /**
     * Repository.getScheduleSummaryForMonth 테스트 (성공 케이스)
     */
    @Test
    fun `getScheduleSummaryForMonth returns success`() = runTest {
        // Arrange
        val startOfMonth = getStartOfMonthTimestamp(testYearMonth)
        val endOfMonth = getEndOfMonthTimestamp(testYearMonth)
        val expectedDateSet = setOf(date1, date2)
        
        // Create timestamps for the test dates
        val timestamp1 = mockk<Timestamp>()
        val timestamp2 = mockk<Timestamp>()
        
        // Create DTOs with these timestamps
        val dto1 = testScheduleDto.copy(id = "dto1", startTime = timestamp1)
        val dto2 = testScheduleDto.copy(id = "dto2", startTime = timestamp2)
        val dtoList = listOf(dto1, dto2)
        
        // Mock the data source call
        coEvery { mockDataSource.getSchedulesForMonth(startOfMonth, endOfMonth) } returns dtoList
        
        // Mock the timestamp->Date conversion for each timestamp
        every { timestamp1.toDate() } returns Date.from(date1.atStartOfDay(zoneId).toInstant())
        every { timestamp2.toDate() } returns Date.from(date2.atStartOfDay(zoneId).toInstant())

        // Act
        val result = scheduleRepository.getScheduleSummaryForMonth(testYearMonth)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedDateSet, result.getOrNull())
        coVerify { mockDataSource.getSchedulesForMonth(startOfMonth, endOfMonth) }
    }

    /**
     * Repository.getSchedulesForDate 실패 테스트 (DataSource 예외 발생)
     */
    @Test
    fun `getSchedulesForDate returns error when data source fails`() = runTest {
        // Arrange
        val exception = Exception("Firestore error")
        val startOfDay = getStartOfDayTimestamp(testDate)
        val endOfDay = getEndOfDayTimestamp(testDate)

        // Create specific mock for this test that throws an exception
        coEvery { mockDataSource.getSchedulesForDate(startOfDay, endOfDay) } throws exception

        // Act
        val result = scheduleRepository.getSchedulesForDate(testDate)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { mockDataSource.getSchedulesForDate(startOfDay, endOfDay) }
    }

    /**
     * Repository.getScheduleSummaryForMonth 실패 테스트 (DataSource 예외 발생)
     */
    @Test
    fun `getScheduleSummaryForMonth returns error when data source fails`() = runTest {
        // Arrange
        val exception = Exception("Firestore error")
        val startOfMonth = getStartOfMonthTimestamp(testYearMonth)
        val endOfMonth = getEndOfMonthTimestamp(testYearMonth)

        // Create specific mock for this test that throws an exception
        coEvery { mockDataSource.getSchedulesForMonth(startOfMonth, endOfMonth) } throws exception

        // Act
        val result = scheduleRepository.getScheduleSummaryForMonth(testYearMonth)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { mockDataSource.getSchedulesForMonth(startOfMonth, endOfMonth) }
    }

    /**
     * 새 일정 추가 테스트 (성공 케이스)
     */
    @Test
    fun `addSchedule returns success`() = runTest {
        // Arrange
        val generatedId = "newScheduleId" // Although repo returns Unit, DS returns ID
        val dtoToAdd = testScheduleDto.copy(id = null)
        every { testScheduleDomain.toDto() } returns dtoToAdd
        coEvery { mockDataSource.addSchedule(dtoToAdd) } returns generatedId // DS returns ID

        // Act
        val result = scheduleRepository.addSchedule(testScheduleDomain)

        // Assert: Repository method returns Result<Unit>
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        verify { testScheduleDomain.toDto() }
         coVerify { mockDataSource.addSchedule(dtoToAdd) }
    }

    /**
     * Repository.addSchedule 실패 테스트 (DataSource 예외 발생)
     */
    @Test
    fun `addSchedule returns error when data source fails`() = runTest {
        // Arrange
        val exception = Exception("Firestore add error")
        val dtoToAdd = testScheduleDto.copy(id = null)
        every { testScheduleDomain.toDto() } returns dtoToAdd
        coEvery { mockDataSource.addSchedule(dtoToAdd) } throws exception

        // Act
        val result = scheduleRepository.addSchedule(testScheduleDomain)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify { testScheduleDomain.toDto() }
        coVerify { mockDataSource.addSchedule(dtoToAdd) }
    }

    /**
     * Repository.updateSchedule 테스트 (성공 케이스)
     */
    @Test
    fun `updateSchedule returns success`() = runTest {
        // Arrange
        val dtoToUpdate = testScheduleDto // Use getter
        every { testScheduleDomain.toDto() } returns dtoToUpdate
        coEvery { mockDataSource.updateSchedule(testScheduleDomain.id, dtoToUpdate) } returns Unit

        // Act
        val result = scheduleRepository.updateSchedule(testScheduleDomain)

        // Assert: Repository method returns Result<Unit>
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        verify { testScheduleDomain.toDto() }
        coVerify { mockDataSource.updateSchedule(testScheduleDomain.id, dtoToUpdate) }
    }

    /**
     * Repository.updateSchedule 실패 테스트 (DataSource 예외 발생)
     */
    @Test
    fun `updateSchedule returns error when data source fails`() = runTest {
        // Arrange
        val exception = Exception("Firestore update error")
        val dtoToUpdate = testScheduleDto // Use getter
        every { testScheduleDomain.toDto() } returns dtoToUpdate
        coEvery { mockDataSource.updateSchedule(testScheduleDomain.id, dtoToUpdate) } throws exception

        // Act
        val result = scheduleRepository.updateSchedule(testScheduleDomain)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify { testScheduleDomain.toDto() }
        coVerify { mockDataSource.updateSchedule(testScheduleDomain.id, dtoToUpdate) }
    }

    /**
     * Repository.deleteSchedule 테스트 (성공 케이스)
     */
    @Test
    fun `deleteSchedule returns success`() = runTest {
        // Arrange
        val scheduleIdToDelete = testScheduleDomain.id
        coEvery { mockDataSource.deleteSchedule(scheduleIdToDelete) } returns Unit

        // Act
        val result = scheduleRepository.deleteSchedule(scheduleIdToDelete)

        // Assert: Repository method returns Result<Unit>
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        coVerify { mockDataSource.deleteSchedule(scheduleIdToDelete) }
    }

    /**
     * Repository.deleteSchedule 실패 테스트 (DataSource 예외 발생)
     */
    @Test
    fun `deleteSchedule returns error when data source fails`() = runTest {
        // Arrange
        val exception = Exception("Firestore delete error")
        val scheduleIdToDelete = testScheduleDomain.id
        coEvery { mockDataSource.deleteSchedule(scheduleIdToDelete) } throws exception

        // Act
        val result = scheduleRepository.deleteSchedule(scheduleIdToDelete)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { mockDataSource.deleteSchedule(scheduleIdToDelete) }
    }
} 