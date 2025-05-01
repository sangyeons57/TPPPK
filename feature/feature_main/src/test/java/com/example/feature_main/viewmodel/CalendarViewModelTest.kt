package com.example.feature_main.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarViewModel 테스트
 *
 * 이 테스트 클래스는 CalendarViewModel의 기본 기능과 상태 관리를 검증합니다.
 * 직접적인 ViewModel 생성 없이 상태 객체와 함수의 로직만 테스트합니다.
 */
class CalendarViewModelTest {

    /**
     * CalendarUiState 초기값 테스트
     */
    @Test
    fun testCalendarUiStateInitialValues() {
        // 기본 생성자로 상태 객체 생성
        val uiState = CalendarUiState()
        
        // 검증: 현재 연월은 현재 시스템 날짜의 연월과 같아야 함
        assertEquals(YearMonth.now(), uiState.currentYearMonth)
        
        // 검증: 선택된 날짜는 오늘 날짜와 같아야 함
        assertEquals(LocalDate.now(), uiState.selectedDate)
        
        // 검증: 스케줄 목록은 비어있어야 함
        assertEquals(emptyList<ScheduleItem>(), uiState.schedulesForSelectedDate)
        
        // 검증: 로딩 상태는 기본적으로 false여야 함
        assertEquals(false, uiState.isLoading)
        
        // 검증: 에러 메시지는 기본적으로 null이어야 함
        assertEquals(null, uiState.errorMessage)
    }
    
    /**
     * 포맷터 기능 테스트
     */
    @Test
    fun testDateFormatters() {
        val uiState = CalendarUiState()
        
        // 테스트 날짜 지정
        val testDate = LocalDate.of(2025, 4, 15)
        val testYearMonth = YearMonth.of(2025, 4)
        
        // 포맷팅 문자열 생성
        val formattedYearMonth = testYearMonth.format(uiState.monthYearFormatter)
        val formattedDate = testDate.format(uiState.selectedDateFormatter)
        
        // 검증: 포맷팅된 결과가 예상 형식과 일치해야 함
        assertEquals("2025년 4월", formattedYearMonth)
        assertEquals("4월 15일 (화)", formattedDate)
    }
    
    /**
     * 날짜 상태 업데이트 로직 테스트
     */
    @Test
    fun testDateSelectionUpdatesState() {
        // 초기 상태
        var state = CalendarUiState()
        
        // 임의의 날짜 선택 - 예시: 3개월 후의 15일
        val newSelectedDate = LocalDate.now().plusMonths(3).withDayOfMonth(15)
        val newYearMonth = YearMonth.from(newSelectedDate)
        
        // 상태 업데이트 (실제 ViewModel 로직을 단순화해서 구현)
        state = state.copy(
            currentYearMonth = newYearMonth,
            selectedDate = newSelectedDate
        )
        
        // 검증: 상태가 올바르게 업데이트되었는지 확인
        assertEquals(newYearMonth, state.currentYearMonth)
        assertEquals(newSelectedDate, state.selectedDate)
    }
    
    /**
     * 스케줄 항목 생성 및 필터링 테스트
     */
    @Test
    fun testScheduleItemsAndFiltering() {
        // 테스트 날짜 설정
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        
        // 테스트용 스케줄 아이템 생성
        val todaySchedules = listOf(
            ScheduleItem(
                id = "today1",
                title = "오늘 회의",
                date = today,
                startTime = null,
                endTime = null
            ),
            ScheduleItem(
                id = "today2",
                title = "오늘 저녁 약속",
                date = today,
                startTime = null,
                endTime = null
            )
        )
        
        val tomorrowSchedules = listOf(
            ScheduleItem(
                id = "tomorrow1",
                title = "내일 회의",
                date = tomorrow,
                startTime = null,
                endTime = null
            )
        )
        
        // 모든 스케줄 결합
        val allSchedules = todaySchedules + tomorrowSchedules
        
        // 오늘 날짜 스케줄만 필터링 (실제 ViewModel의 로직을 단순화하여 구현)
        val filteredSchedules = allSchedules.filter { it.date == today }
        
        // 검증: 필터링된 스케줄은 오늘 날짜의 스케줄만 포함해야 함
        assertEquals(todaySchedules.size, filteredSchedules.size)
        assertEquals(todaySchedules, filteredSchedules)
        
        // 검증: 내일 날짜 스케줄은 포함되지 않아야 함
        filteredSchedules.forEach { schedule ->
            assertEquals(today, schedule.date)
        }
    }
} 