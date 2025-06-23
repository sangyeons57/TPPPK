package com.example.feature_main.ui.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.feature_calendar.CalendarUiState
import com.example.feature_calendar.CalendarViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CalendarScreen 컴포넌트 테스트
 *
 * 이 테스트 클래스는 CalendarScreen의 UI 표시 및 이벤트 처리를 검증합니다.
 * 모의 ViewModel을 사용하여 다양한 상태에서 화면이 올바르게 렌더링되는지 확인합니다.
 */
@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

    /**
     * Compose UI 테스트를 위한 Rule
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 모의 ViewModel 객체
     */
    private lateinit var mockViewModel: com.example.feature_calendar.CalendarViewModel

    /**
     * 테스트용 UI 상태 Flow
     */
    private val mockUiStateFlow = MutableStateFlow(com.example.feature_calendar.CalendarUiState())

    /**
     * 테스트 설정
     */
    @Before
    fun setup() {
        // 모의 ViewModel 설정
        mockViewModel = mock(com.example.feature_calendar.CalendarViewModel::class.java)
        `when`(mockViewModel.uiState).thenReturn(mockUiStateFlow)
    }

    /**
     * 캘린더 화면이 기본 상태에서 올바르게 표시되는지 테스트
     */
    @Test
    fun calendarScreen_defaultState_displaysCorrectElements() {
        // 현재 날짜 기반 테스트 데이터
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val monthYearFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
        val currentMonthStr = currentMonth.format(monthYearFormatter)

        // 모의 UI 상태 설정
        val testState = com.example.feature_calendar.CalendarUiState(
            currentYearMonth = currentMonth,
            selectedDate = today,
            datesInMonth = generateMockDates(currentMonth)
        )
        mockUiStateFlow.value = testState

        // CalendarScreen 표시
        composeTestRule.setContent {
            com.example.feature_calendar.CalendarScreen(
                onClickFAB = {},
                onNavigateToScheduleDetail = {},
                onNavigateToCalendar24Hour = { _, _, _ -> },
                viewModel = mockViewModel
            )
        }

        // 기본 요소들이 표시되는지 확인
        composeTestRule.onNodeWithText(currentMonthStr).assertIsDisplayed()

        // 날짜 포맷 확인 (예: "4월 15일 (목)" 형식)
        val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
        val formattedDate = today.format(dateFormatter)
        
        // "X일 일정" 텍스트 확인 (X는 선택된 날짜)
        composeTestRule.onNodeWithText("$formattedDate 일정", substring = true).assertIsDisplayed()
        
        // 24시간 보기 버튼 표시 확인
        composeTestRule.onNodeWithText("24시간 보기").assertIsDisplayed()
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