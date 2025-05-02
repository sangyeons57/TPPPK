package com.example.feature_main.ui.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature_main.viewmodel.ScheduleItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CalendarComponents 테스트
 *
 * 이 테스트 클래스는 개별 캘린더 UI 컴포넌트의 기능과 렌더링을 검증합니다.
 * - MonthHeader: 월 표시 및 이동 버튼
 * - DayOfWeekHeader: 요일 표시
 * - CalendarGrid: 날짜 그리드
 * - DayCell: 날짜 셀
 * - ScheduleSection: 일정 목록 영역
 */
@RunWith(JUnit4::class)
class CalendarComponentsTest {

    /**
     * Compose 테스트 규칙
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * MonthHeader 컴포넌트 렌더링 테스트
     *
     * 이 테스트는 MonthHeader 컴포넌트가 다음을 올바르게 렌더링하는지 확인합니다:
     * - 현재 연/월이 올바른 형식으로 표시되는지
     * - 이전/다음 월 이동 버튼이 존재하는지
     */
    @Test
    fun `MonthHeader displays correct month and navigation buttons`() {
        // Given: 테스트할 월과 연도
        val testYearMonth = YearMonth.of(2023, 10)
        val expectedMonthText = "2023년 10월"
        
        // When: MonthHeader 컴포넌트를 렌더링
        composeTestRule.setContent {
            MonthHeader(
                currentYearMonth = testYearMonth,
                onPreviousMonthClick = { },
                onNextMonthClick = { }
            )
        }
        
        // Then: 월 표시 및 네비게이션 버튼이 올바르게 렌더링되었는지 확인
        composeTestRule.onNodeWithText(expectedMonthText).assertExists()
        composeTestRule.onNodeWithContentDescription("이전 달").assertExists()
        composeTestRule.onNodeWithContentDescription("다음 달").assertExists()
    }
    
    /**
     * DayOfWeekHeader 컴포넌트 렌더링 테스트
     *
     * 이 테스트는 DayOfWeekHeader 컴포넌트가 모든 요일을 올바르게 표시하는지 확인합니다.
     */
    @Test
    fun `DayOfWeekHeader displays all days of week`() {
        // When: DayOfWeekHeader 컴포넌트를 렌더링
        composeTestRule.setContent {
            DayOfWeekHeader()
        }
        
        // Then: 모든 요일이 올바르게 표시되는지 확인
        val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
        daysOfWeek.forEach { day ->
            composeTestRule.onNodeWithText(day).assertExists()
        }
    }
    
    /**
     * DayCell 컴포넌트 렌더링 테스트 - 선택된 날짜
     *
     * 이 테스트는 DayCell 컴포넌트가 선택된 날짜를 올바르게 표시하는지 확인합니다.
     */
    @Test
    fun `DayCell displays selected date correctly`() {
        // Given: 테스트할 날짜 및 상태
        val today = LocalDate.now()
        val testDate = today
        val isSelected = true
        
        // When: DayCell 컴포넌트를 렌더링
        composeTestRule.setContent {
            DayCell(
                date = testDate,
                isSelected = isSelected,
                isToday = true,
                hasSchedules = false,
                onDateClick = { }
            )
        }
        
        // Then: 날짜가 선택됨 상태로 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText(testDate.dayOfMonth.toString())
            .assertIsDisplayed()
            .assertHasNoClickAction() // 선택된 상태에서는 클릭 액션이 없어야 함
    }
    
    /**
     * DayCell 컴포넌트 렌더링 테스트 - 일정이 있는 날짜
     *
     * 이 테스트는 DayCell 컴포넌트가 일정이 있는 날짜를 올바르게 표시하는지 확인합니다.
     */
    @Test
    fun `DayCell displays date with schedules correctly`() {
        // Given: 테스트할 날짜 및 상태
        val today = LocalDate.now()
        val testDate = today.plusDays(1) // 내일
        
        // When: 일정이 있는 DayCell 컴포넌트를 렌더링
        composeTestRule.setContent {
            DayCell(
                date = testDate,
                isSelected = false,
                isToday = false,
                hasSchedules = true,
                onDateClick = { }
            )
        }
        
        // Then: 날짜가 일정이 있는 상태로 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText(testDate.dayOfMonth.toString())
            .assertIsDisplayed()
    }
    
    /**
     * ScheduleSection 컴포넌트 렌더링 테스트
     *
     * 이 테스트는 ScheduleSection 컴포넌트가 일정 목록을 올바르게 표시하는지 확인합니다.
     */
    @Test
    fun `ScheduleSection displays schedule items correctly`() {
        // Given: 테스트할 일정 목록
        val testDate = LocalDate.now()
        val schedules = listOf(
            ScheduleItem(
                id = "1",
                title = "테스트 일정 1",
                description = "테스트 설명 1",
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                date = testDate,
                color = 0xFFE57373.toInt()
            ),
            ScheduleItem(
                id = "2",
                title = "테스트 일정 2",
                description = "테스트 설명 2",
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 30),
                date = testDate,
                color = 0xFF81C784.toInt()
            )
        )
        
        // When: ScheduleSection 컴포넌트를 렌더링
        composeTestRule.setContent {
            ScheduleSection(
                schedules = schedules,
                onScheduleClick = { }
            )
        }
        
        // Then: 일정 항목이 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("테스트 일정 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("테스트 일정 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("09:00-10:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("14:00-15:30").assertIsDisplayed()
    }
} 