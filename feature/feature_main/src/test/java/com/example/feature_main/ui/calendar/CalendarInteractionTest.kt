package com.example.feature_main.ui.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_calendar.CalendarUiState
import com.example.feature_main.viewmodel.ScheduleItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * 캘린더 사용자 상호작용 테스트
 *
 * 이 테스트 클래스는 캘린더 UI와 사용자 간의 상호작용을 검증합니다.
 * Mockito 대신 직접 콜백 확인 방식으로 구현되었습니다.
 */
@RunWith(JUnit4::class)
class CalendarInteractionTest {

    /**
     * Compose 테스트 규칙
     */
    @get:Rule
    val composeTestRule = createComposeRule()
    
    // 테스트 데이터
    private val testYearMonth = YearMonth.now()
    private val testDate = LocalDate.now()
    private val testSchedules = listOf(
        ScheduleItem(
            id = "1",
            title = "중요 회의",
            description = "프로젝트 진행 상황 공유",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 30),
            date = testDate,
            color = 0xFFE57373.toInt()
        )
    )
    
    // 콜백 추적을 위한 변수들
    private var dateClickedCount = 0
    private var clickedDate: LocalDate? = null
    private var previousMonthClickedCount = 0
    private var nextMonthClickedCount = 0
    private var scheduleClickedCount = 0
    private var clickedScheduleId: String? = null
    
    @Before
    fun setup() {
        // 콜백 추적 변수 초기화
        dateClickedCount = 0
        clickedDate = null
        previousMonthClickedCount = 0
        nextMonthClickedCount = 0
        scheduleClickedCount = 0
        clickedScheduleId = null
    }
    
    /**
     * 날짜 선택 테스트
     */
    @Test
    fun `when date is clicked then callback is invoked correctly`() {
        // Given: 테스트할 날짜와 상태
        val clickableDate = testDate.plusDays(2)
        val state = com.example.feature_calendar.CalendarUiState(
            currentYearMonth = testYearMonth,
            selectedDate = testDate,
            schedules = testSchedules
        )
        
        // When: CalendarGrid 컴포넌트를 렌더링하고 날짜 클릭
        composeTestRule.setContent {
            com.example.feature_calendar.CalendarGrid(
                currentYearMonth = state.currentYearMonth,
                selectedDate = state.selectedDate,
                schedulesByDate = state.schedulesByDate,
                onDateClick = { date ->
                    dateClickedCount++
                    clickedDate = date
                }
            )
        }
        
        // Then: 클릭 가능한 날짜를 찾아 클릭
        composeTestRule.onNodeWithText(clickableDate.dayOfMonth.toString())
            .performClick()
        
        // 콜백이 올바르게 호출되었는지 확인
        assert(dateClickedCount == 1) { "날짜 클릭 콜백이 호출되지 않았습니다." }
        assert(clickedDate == clickableDate) { "클릭된 날짜가, 클릭한 날짜와 일치하지 않습니다." }
    }
    
    /**
     * 월 이동 테스트
     */
    @Test
    fun `when navigation buttons are clicked then callbacks are invoked correctly`() {
        // Given: 테스트할 상태
        val state = com.example.feature_calendar.CalendarUiState(
            currentYearMonth = testYearMonth,
            selectedDate = testDate,
            schedules = testSchedules
        )
        
        // When: MonthHeader 컴포넌트를 렌더링
        composeTestRule.setContent {
            com.example.feature_calendar.MonthHeader(
                currentYearMonth = state.currentYearMonth,
                onPreviousMonthClick = {
                    previousMonthClickedCount++
                },
                onNextMonthClick = {
                    nextMonthClickedCount++
                }
            )
        }
        
        // Then: 이전 달 버튼 클릭
        composeTestRule.onNodeWithContentDescription("이전 달")
            .performClick()
        
        // 이전 달 콜백이 호출되었는지 확인
        assert(previousMonthClickedCount == 1) { "이전 달 클릭 콜백이 호출되지 않았습니다." }
        
        // 다음 달 버튼 클릭
        composeTestRule.onNodeWithContentDescription("다음 달")
            .performClick()
        
        // 다음 달 콜백이 호출되었는지 확인
        assert(nextMonthClickedCount == 1) { "다음 달 클릭 콜백이 호출되지 않았습니다." }
    }
    
    /**
     * 일정 클릭 테스트
     */
    @Test
    fun `when schedule item is clicked then callback is invoked correctly`() {
        // When: ScheduleSection 컴포넌트를 렌더링
        composeTestRule.setContent {
            com.example.feature_calendar.ScheduleSection(
                schedules = testSchedules,
                onScheduleClick = { scheduleId ->
                    scheduleClickedCount++
                    clickedScheduleId = scheduleId
                }
            )
        }
        
        // Then: 일정 항목 클릭
        composeTestRule.onNodeWithText("중요 회의")
            .performClick()
        
        // 일정 클릭 콜백이 호출되었는지 확인
        assert(scheduleClickedCount == 1) { "일정 클릭 콜백이 호출되지 않았습니다." }
        assert(clickedScheduleId == "1") { "클릭된 일정 ID가 올바르지 않습니다." }
    }
    
    /**
     * 전체 캘린더 화면 렌더링 테스트
     */
    @Test
    fun `CalendarScreen renders all components correctly`() {
        // Given: 필요한 콜백
        var scheduleClicked = false
        
        // When: 전체 CalendarScreen 컴포넌트를 렌더링
        composeTestRule.setContent {
            com.example.feature_calendar.CalendarContent(
                uiState = com.example.feature_calendar.CalendarUiState(
                    currentYearMonth = testYearMonth,
                    selectedDate = testDate,
                    schedules = testSchedules
                ),
                onDateClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onScheduleClick = { scheduleClicked = true }
            )
        }
        
        // Then: 화면의 주요 요소들이 존재하는지 확인
        // 월 헤더 확인
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월")
        val monthText = testYearMonth.format(formatter)
        composeTestRule.onNodeWithText(monthText).assertExists()
        
        // 요일 헤더 확인
        composeTestRule.onNodeWithText("일").assertExists()
        composeTestRule.onNodeWithText("토").assertExists()
        
        // 날짜 그리드 확인
        composeTestRule.onNodeWithText(testDate.dayOfMonth.toString()).assertExists()
        
        // 일정 섹션 확인
        composeTestRule.onNodeWithText("중요 회의").assertExists()
    }
} 