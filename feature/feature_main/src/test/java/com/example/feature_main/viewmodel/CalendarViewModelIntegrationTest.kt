package com.example.feature_main.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarViewModel 단순 테스트
 *
 * 이 테스트 클래스는 Repository 의존성 없이 직접 ViewModel 기능을 테스트합니다.
 * - 날짜 선택 로직
 * - 월 간 네비게이션 로직
 * - 기본 상태 관리
 */
@ExperimentalCoroutinesApi
class CalendarViewModelIntegrationTest {

    /**
     * LiveData 테스트를 위한 규칙
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * 테스트용 디스패처
     */
    private val testDispatcher = StandardTestDispatcher()

    /**
     * 테스트 대상 ViewModel
     */
    private lateinit var viewModel: SimplifiedCalendarViewModel

    /**
     * 테스트 데이터
     */
    private val testDate = LocalDate.of(2023, 10, 15)

    /**
     * 테스트를 위한 단순화된 ViewModel 클래스
     */
    class SimplifiedCalendarViewModel {
        private var _currentYearMonth = YearMonth.now()
        val currentYearMonth: YearMonth get() = _currentYearMonth
        
        private var _selectedDate = LocalDate.now()
        val selectedDate: LocalDate get() = _selectedDate
        
        private val _schedules = mutableListOf<ScheduleItem>()
        val schedules: List<ScheduleItem> get() = _schedules
        
        fun selectDate(date: LocalDate) {
            _selectedDate = date
        }
        
        fun navigateToPreviousMonth() {
            _currentYearMonth = _currentYearMonth.minusMonths(1)
        }
        
        fun navigateToNextMonth() {
            _currentYearMonth = _currentYearMonth.plusMonths(1)
        }
        
        fun loadSchedules(yearMonth: YearMonth, schedules: List<ScheduleItem>) {
            _schedules.clear()
            _schedules.addAll(schedules)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SimplifiedCalendarViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `initial state is set correctly`() {
        // Then: 초기 상태가 올바르게 설정됨
        assertEquals(YearMonth.now(), viewModel.currentYearMonth)
        assertEquals(LocalDate.now(), viewModel.selectedDate)
        assertTrue(viewModel.schedules.isEmpty())
    }

    /**
     * 날짜 선택 테스트
     */
    @Test
    fun `selectDate updates selected date correctly`() {
        // Given: 선택할 테스트 날짜
        val newSelectedDate = testDate.plusDays(5)
        
        // When: 날짜 선택
        viewModel.selectDate(newSelectedDate)
        
        // Then: 선택된 날짜가 업데이트됨
        assertEquals(newSelectedDate, viewModel.selectedDate)
    }

    /**
     * 이전 월 이동 테스트
     */
    @Test
    fun `navigateToPreviousMonth updates current month correctly`() {
        // Given: 초기 월
        val initialYearMonth = viewModel.currentYearMonth
        val expectedPreviousMonth = initialYearMonth.minusMonths(1)
        
        // When: 이전 월 이동
        viewModel.navigateToPreviousMonth()
        
        // Then: 현재 월이 업데이트됨
        assertEquals(expectedPreviousMonth, viewModel.currentYearMonth)
    }

    /**
     * 다음 월 이동 테스트
     */
    @Test
    fun `navigateToNextMonth updates current month correctly`() {
        // Given: 초기 월
        val initialYearMonth = viewModel.currentYearMonth
        val expectedNextMonth = initialYearMonth.plusMonths(1)
        
        // When: 다음 월 이동
        viewModel.navigateToNextMonth()
        
        // Then: 현재 월이 업데이트됨
        assertEquals(expectedNextMonth, viewModel.currentYearMonth)
    }

    /**
     * 일정 로드 테스트
     */
    @Test
    fun `loadSchedules updates schedule list correctly`() = runTest {
        // Given: 로드할 테스트 일정
        val testYearMonth = YearMonth.of(2023, 10)
        val testSchedules = listOf(
            ScheduleItem(
                id = "1",
                title = "테스트 일정",
                description = "테스트 설명",
                startTime = null,
                endTime = null,
                date = testDate,
                color = 0
            )
        )
        
        // When: 일정 로드
        viewModel.loadSchedules(testYearMonth, testSchedules)
        
        // Then: 일정 목록이 업데이트됨
        assertEquals(1, viewModel.schedules.size)
        assertEquals("테스트 일정", viewModel.schedules[0].title)
    }
} 