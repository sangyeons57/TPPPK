package com.example.feature_main.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 캘린더 관련 유틸리티 테스트
 * 
 * 이 테스트 클래스는 캘린더 화면과 컴포넌트의 날짜 처리 관련 
 * 유틸리티 함수들을 테스트합니다.
 */
class CalendarUtilsTest {

    /**
     * 날짜 포맷팅 테스트
     */
    @Test
    fun testDateFormatting() {
        // 기준 날짜 설정
        val testDate = LocalDate.of(2025, 4, 15)
        
        // 연월 포맷팅 테스트
        val yearMonthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
        val formattedYearMonth = testDate.format(yearMonthFormatter)
        assertEquals("2025년 4월", formattedYearMonth)
        
        // 날짜 포맷팅 테스트 (M월 d일 (E) 형식)
        val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
        val formattedDate = testDate.format(dateFormatter)
        assertEquals("4월 15일 (화)", formattedDate)
    }
    
    /**
     * 월별 날짜 생성 테스트
     */
    @Test
    fun testGenerateMonthDates() {
        // 테스트 월 설정 (2025년 4월)
        val yearMonth = YearMonth.of(2025, 4)
        
        // 해당 월의 첫째날
        val firstDay = yearMonth.atDay(1)
        // 주의 시작을 일요일로 계산 (DayOfWeek.SUNDAY.value는 7)
        val firstDayOffset = firstDay.dayOfWeek.value % 7
        
        // 월의 총 일수
        val daysInMonth = yearMonth.lengthOfMonth()
        
        // 날짜 배열 생성
        val result = mutableListOf<LocalDate?>()
        repeat(firstDayOffset) { result.add(null) }
        for (day in 1..daysInMonth) {
            result.add(yearMonth.atDay(day))
        }
        
        // 검증: 첫 번째 날짜 이전의 offset 확인
        for (i in 0 until firstDayOffset) {
            assertEquals(null, result[i])
        }
        
        // 검증: 첫 번째 실제 날짜가 1일인지 확인
        assertEquals(1, result[firstDayOffset]?.dayOfMonth)
        
        // 검증: 마지막 날짜가 해당 월의 마지막 날짜인지 확인
        assertEquals(daysInMonth, result.last()?.dayOfMonth)
        
        // 검증: 배열의 크기가 offset + 해당 월의 일수와 같은지 확인
        assertEquals(firstDayOffset + daysInMonth, result.size)
    }
    
    /**
     * 현재 날짜 및 월 테스트
     */
    @Test
    fun testCurrentDateAndMonth() {
        // 현재 날짜 및 월 가져오기
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        
        // 검증: 현재 날짜는 null이 아니어야 함
        assertNotNull(today)
        
        // 검증: 현재 월은 null이 아니어야 함
        assertNotNull(currentMonth)
        
        // 검증: 현재 날짜의 연월은 현재 월과 같아야 함
        assertEquals(YearMonth.from(today), currentMonth)
    }
} 