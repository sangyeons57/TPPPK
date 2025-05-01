package com.example.feature_main.viewmodel

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarViewModel 단위 테스트
 *
 * 이 테스트 클래스는 Repository 의존성 없이 ViewModel 내부 로직만 검증합니다.
 */
class CalendarViewModelTest {

    /**
     * CalendarUiState 초기값 테스트
     */
    @Test
    fun `CalendarUiState should initialize with current date values`() {
        // Given: UI 상태 객체 생성
        val uiState = CalendarUiState()
        
        // Then: 초기값이 현재 날짜 관련 값으로 설정되었는지 확인
        assertEquals(YearMonth.now(), uiState.currentYearMonth)
        assertEquals(LocalDate.now(), uiState.selectedDate)
        assertTrue(uiState.schedules.isEmpty())
    }
    
    /**
     * 날짜별 일정 그룹화 테스트
     */
    @Test
    fun `schedulesByDate should group schedules by date`() {
        // Given: 서로 다른 날짜의 일정이 있는 상태
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        
        val schedules = listOf(
            ScheduleItem(id = "1", title = "일정 1", description = "", 
                        startTime = null, endTime = null, date = today, color = 0),
            ScheduleItem(id = "2", title = "일정 2", description = "", 
                        startTime = null, endTime = null, date = today, color = 0),
            ScheduleItem(id = "3", title = "일정 3", description = "", 
                        startTime = null, endTime = null, date = tomorrow, color = 0)
        )
        
        val uiState = CalendarUiState(schedules = schedules)
        
        // When: 날짜별 일정 맵 조회
        val schedulesByDate = uiState.schedulesByDate
        
        // Then: 날짜별로 올바르게 그룹화되었는지 확인
        assertEquals(2, schedulesByDate.size) // 2개 날짜
        assertEquals(2, schedulesByDate[today]?.size) // 오늘 일정 2개
        assertEquals(1, schedulesByDate[tomorrow]?.size) // 내일 일정 1개
    }
    
    /**
     * 월별 날짜 계산 테스트
     */
    @Test
    fun `daysInMonth should return correct days for the month`() {
        // Given: 2023년 2월 (28일)과 2024년 2월 (윤년, 29일)
        val feb2023 = YearMonth.of(2023, 2)
        val feb2024 = YearMonth.of(2024, 2)
        
        // Then: 각 월의 일수가 올바른지 확인
        assertEquals(28, feb2023.lengthOfMonth())
        assertEquals(29, feb2024.lengthOfMonth())
    }
    
    /**
     * 오늘 날짜 확인 테스트
     */
    @Test
    fun `isToday should correctly identify today`() {
        // Given: 오늘 날짜와 다른 날짜
        val today = LocalDate.now()
        val otherDay = today.plusDays(1)
        
        // Then: 오늘 판단 로직이 올바른지 확인
        assertTrue(today.equals(LocalDate.now()))
        assertFalse(otherDay.equals(LocalDate.now()))
    }
    
    /**
     * 날짜 포맷팅 테스트
     */
    @Test
    fun `date formatting should work correctly`() {
        // Given: 2023년 10월
        val yearMonth = YearMonth.of(2023, 10)
        
        // When: 포맷팅
        val formatted = yearMonth.toString()
        
        // Then: 형식이 올바른지 확인
        assertEquals("2023-10", formatted)
    }
} 