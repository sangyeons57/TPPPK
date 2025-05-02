package com.example.feature_main.ui.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.feature_main.viewmodel.CalendarUiState
import com.example.feature_main.viewmodel.ScheduleItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
 * - ScheduleListItem: 개별 일정 아이템
 */
@RunWith(AndroidJUnit4::class)
class CalendarComponentsTest {

    /**
     * Compose UI 테스트를 위한 Rule
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * MonthHeader 컴포넌트의 렌더링 및 클릭 이벤트를 테스트
     */
    @Test
    fun monthHeader_rendersCorrectly_andHandlesClicks() {
        // 클릭 이벤트 확인용 변수
        var previousClicked = false
        var nextClicked = false

        // 컴포넌트 표시
        composeTestRule.setContent {
            MonthHeader(
                yearMonthText = "2025년 4월",
                onPreviousClick = { previousClicked = true },
                onNextClick = { nextClicked = true }
            )
        }

        // 텍스트 확인
        composeTestRule.onNodeWithText("2025년 4월").assertIsDisplayed()

        // 이전 버튼 클릭 테스트
        composeTestRule.onNodeWithContentDescription("이전 달").performClick()
        assert(previousClicked) { "이전 달 버튼이 클릭되었지만 이벤트가 처리되지 않았습니다." }

        // 다음 버튼 클릭 테스트
        composeTestRule.onNodeWithContentDescription("다음 달").performClick()
        assert(nextClicked) { "다음 달 버튼이 클릭되었지만 이벤트가 처리되지 않았습니다." }
    }

    /**
     * DayOfWeekHeader 컴포넌트가 모든 요일을 올바르게 표시하는지 테스트
     */
    @Test
    fun dayOfWeekHeader_displaysCorrectDays() {
        // 컴포넌트 표시
        composeTestRule.setContent {
            DayOfWeekHeader()
        }

        // 요일 확인 (한국어 표기)
        val daysInKorean = listOf("일", "월", "화", "수", "목", "금", "토")
        daysInKorean.forEach { day ->
            composeTestRule.onNodeWithText(day).assertIsDisplayed()
        }
    }

    /**
     * ScheduleListItem 컴포넌트 테스트
     */
    @Test
    fun scheduleListItem_rendersCorrectly() {
        // 테스트 일정 데이터
        val testSchedule = ScheduleItem(
            id = "test1",
            title = "중요한 회의",
            date = LocalDate.now(),
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 30),
            color = 0xFFEF5350
        )
        
        var itemClicked = false
        
        // 컴포넌트 표시
        composeTestRule.setContent {
            ScheduleListItem(
                schedule = testSchedule,
                onClick = { itemClicked = true }
            )
        }
        
        // 일정 제목 표시 확인
        composeTestRule.onNodeWithText("중요한 회의").assertIsDisplayed()
        
        // 클릭 테스트
        composeTestRule.onNodeWithText("중요한 회의").performClick()
        assert(itemClicked) { "일정 아이템이 클릭되었지만 이벤트가 처리되지 않았습니다." }
    }
    
    /**
     * 테스트용 날짜 배열 생성
     */
    private fun generateMockDates(yearMonth: YearMonth): List<LocalDate?> {
        val result = mutableListOf<LocalDate?>()
        
        // 첫 날 offset 계산 (일요일 시작, 0-6)
        val firstDay = yearMonth.atDay(1)
        val firstDayOffset = firstDay.dayOfWeek.value % 7
        
        // 첫 날 전 빈칸
        repeat(firstDayOffset) { result.add(null) }
        
        // 해당 월의 날짜들
        for (day in 1..yearMonth.lengthOfMonth()) {
            result.add(yearMonth.atDay(day))
        }
        
        return result
    }
} 