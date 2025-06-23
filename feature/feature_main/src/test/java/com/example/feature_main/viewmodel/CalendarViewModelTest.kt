package com.example.feature_main.viewmodel

import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.Schedule
import com.example.domain.repository.base.ScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 CalendarViewModel의 기능을 검증합니다.
 * Fake 구현체를 통해 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class CalendarViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: com.example.feature_calendar.CalendarViewModel

    // Fake Repository
    private lateinit var fakeScheduleRepository: FakeScheduleRepository

    // 테스트 데이터
    private val today = LocalDate.now()
    private val testDate = LocalDate.of(2023, 5, 15)
    private val testSchedule1 = Schedule(
        id = "schedule_1",
        title = "테스트 일정 1",
        content = "테스트 내용 1",
        startTime = LocalDate.of(2023, 5, 15).atTime(10, 0),
        endTime = LocalDate.of(2023, 5, 15).atTime(11, 0),
        isAllDay = false,
        projectId = null,
        attendees = emptyList()
    )
    private val testSchedule2 = Schedule(
        id = "schedule_2",
        title = "테스트 일정 2",
        content = "테스트 내용 2",
        startTime = LocalDate.of(2023, 5, 15).atTime(14, 0),
        endTime = LocalDate.of(2023, 5, 15).atTime(15, 0),
        isAllDay = false,
        projectId = null,
        attendees = emptyList()
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // Fake Repository 초기화
        fakeScheduleRepository = FakeScheduleRepository()

        // 테스트 데이터 설정
        fakeScheduleRepository.addSchedule(testSchedule1)
        fakeScheduleRepository.addSchedule(testSchedule2)

        // ViewModel 초기화 (의존성 주입)
        viewModel = com.example.feature_calendar.CalendarViewModel(fakeScheduleRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 현재 날짜와 월을 사용해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals(YearMonth.now(), initialState.currentYearMonth)
        assertEquals(LocalDate.now(), initialState.selectedDate)
        assertFalse(initialState.isLoading)
        assertNull(initialState.errorMessage)
    }

    /**
     * 이전 달 이동 테스트
     */
    @Test
    fun `이전 달로 이동하면 현재 월이 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val initialMonth = viewModel.uiState.getValue().currentYearMonth

        // When: 이전 달로 이동
        viewModel.onPreviousMonthClick()

        // Then: 월이 업데이트됨
        val newMonth = viewModel.uiState.getValue().currentYearMonth
        assertEquals(initialMonth.minusMonths(1), newMonth)
    }

    /**
     * 다음 달 이동 테스트
     */
    @Test
    fun `다음 달로 이동하면 현재 월이 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val initialMonth = viewModel.uiState.getValue().currentYearMonth

        // When: 다음 달로 이동
        viewModel.onNextMonthClick()

        // Then: 월이 업데이트됨
        val newMonth = viewModel.uiState.getValue().currentYearMonth
        assertEquals(initialMonth.plusMonths(1), newMonth)
    }

    /**
     * 날짜 선택 테스트
     */
    @Test
    fun `날짜 선택 시 선택된 날짜가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val initialDate = viewModel.uiState.getValue().selectedDate
        val newDate = initialDate.plusDays(5)

        // When: 새 날짜 선택
        viewModel.onDateSelected(newDate)

        // Then: 선택된 날짜 업데이트됨
        val updatedDate = viewModel.uiState.getValue().selectedDate
        assertEquals(newDate, updatedDate)
    }

    /**
     * 일정 클릭 시 이벤트 발생 테스트
     */
    @Test
    fun `일정 클릭 시 NavigateToScheduleDetail 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_calendar.CalendarEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 일정 클릭
        val scheduleId = "test_schedule_id"
        viewModel.onScheduleClick(scheduleId)

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is com.example.feature_calendar.CalendarEvent.NavigateToScheduleDetail)
        assertEquals(scheduleId, (event as com.example.feature_calendar.CalendarEvent.NavigateToScheduleDetail).scheduleId)
    }

    /**
     * 일정 추가 버튼 클릭 테스트
     */
    @Test
    fun `일정 추가 버튼 클릭 시 ShowAddScheduleDialog 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_calendar.CalendarEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 일정 추가 버튼 클릭
        viewModel.onAddScheduleClick()

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is com.example.feature_calendar.CalendarEvent.ShowAddScheduleDialog)
    }

    /**
     * 에러 메시지 표시 후 초기화 테스트
     */
    @Test
    fun `에러 메시지 표시 후 초기화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러 메시지가 있는 상태
        val errorMessage = "테스트 에러 메시지"
        viewModel._uiState.value = viewModel.uiState.getValue().copy(errorMessage = errorMessage)
        assertEquals(errorMessage, viewModel.uiState.getValue().errorMessage)

        // When: 에러 메시지 표시 완료 호출
        viewModel.errorMessageShown()

        // Then: 에러 메시지가 null로 초기화됨
        assertNull(viewModel.uiState.getValue().errorMessage)
    }

    /**
     * Repository 오류 테스트
     */
    @Test
    fun `Repository에서 오류 발생 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: Repository에서 오류가 발생하도록 설정
        val testError = Exception("테스트 에러")
        fakeScheduleRepository.setShouldSimulateError(true, testError)

        // When: 날짜 선택 (일정 로드 트리거)
        viewModel.onDateSelected(testDate)

        // Then: 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("스케줄 로드 실패", state.errorMessage)
    }

    /**
     * 일정 수에 따른 마커 표시 테스트
     */
    @Test
    fun `일정이 있는 날짜에는 마커가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 여러 날짜에 일정 추가
        val date1 = LocalDate.of(2023, 6, 10)
        val date2 = LocalDate.of(2023, 6, 15)
        val date3 = LocalDate.of(2023, 6, 20)
        
        fakeScheduleRepository.addSchedule(
            Schedule(
                id = "schedule_test_1",
                title = "테스트 일정 1",
                content = "내용",
                startTime = date1.atTime(10, 0),
                endTime = date1.atTime(11, 0),
                isAllDay = false,
                projectId = null,
                attendees = emptyList()
            )
        )
        
        fakeScheduleRepository.addSchedule(
            Schedule(
                id = "schedule_test_2",
                title = "테스트 일정 2",
                content = "내용",
                startTime = date2.atTime(10, 0),
                endTime = date2.atTime(11, 0),
                isAllDay = false,
                projectId = null,
                attendees = emptyList()
            )
        )
        
        fakeScheduleRepository.addSchedule(
            Schedule(
                id = "schedule_test_3",
                title = "테스트 일정 3",
                content = "내용",
                startTime = date3.atTime(10, 0),
                endTime = date3.atTime(11, 0),
                isAllDay = false,
                projectId = null,
                attendees = emptyList()
            )
        )

        // When: 해당 월로 이동
        viewModel.onDateSelected(date1)

        // Then: 일정이 있는 날짜에 마커가 표시됨
        val datesWithSchedules = viewModel.uiState.getValue().datesWithSchedules
        assertTrue(datesWithSchedules.contains(date1))
        assertTrue(datesWithSchedules.contains(date2))
        assertTrue(datesWithSchedules.contains(date3))
    }
}

/**
 * CalendarViewModel 테스트용 FakeScheduleRepository
 */
class FakeScheduleRepository : ScheduleRepository {
    private val schedules = mutableListOf<Schedule>()
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")

    fun addSchedule(schedule: Schedule) {
        schedules.add(schedule)
    }

    fun clearSchedules() {
        schedules.clear()
    }

    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }

    override suspend fun getSchedulesForDate(date: LocalDate): Result<List<Schedule>> {
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }
        
        return Result.success(
            schedules.filter { schedule ->
                val scheduleDate = schedule.startTime.toLocalDate()
                scheduleDate == date
            }
        )
    }

    override suspend fun getScheduleSummaryForMonth(month: YearMonth): Result<Set<LocalDate>> {
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }

        val datesWithSchedules = schedules
            .map { it.startTime.toLocalDate() }
            .filter { 
                val scheduleMonth = YearMonth.from(it)
                scheduleMonth == month
            }
            .toSet()
            
        return Result.success(datesWithSchedules)
    }

    override suspend fun getScheduleDetail(scheduleId: String): Result<Schedule> {
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }
        
        val schedule = schedules.find { it.id == scheduleId }
        return if (schedule != null) {
            Result.success(schedule)
        } else {
            Result.failure(NoSuchElementException("Schedule not found: $scheduleId"))
        }
    }

    override suspend fun addSchedule(schedule: Schedule): Result<Unit> {
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }
        
        schedules.add(schedule)
        return Result.success(Unit)
    }

    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }
        
        val removed = schedules.removeIf { it.id == scheduleId }
        return if (removed) {
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Schedule not found: $scheduleId"))
        }
    }

    override suspend fun updateSchedule(schedule: Schedule): Result<Unit> {
        if (shouldSimulateError) {
            return Result.failure(errorToSimulate)
        }
        
        val index = schedules.indexOfFirst { it.id == schedule.id }
        return if (index >= 0) {
            schedules[index] = schedule
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Schedule not found: ${schedule.id}"))
        }
    }
} 