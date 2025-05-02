package com.example.feature_main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// --- 데이터 모델 (예시) ---
data class ScheduleItem(
    val id: String,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val color: Long = 0xFF03A9F4 // 예시: 기본 색상 (ARGB Long)
)
// ------------------------

// 캘린더 UI 상태
data class CalendarUiState(
    val currentYearMonth: YearMonth = YearMonth.now(), // 현재 표시 중인 연/월
    val selectedDate: LocalDate = LocalDate.now(), // 현재 선택된 날짜
    val schedulesForSelectedDate: List<ScheduleItem> = emptyList(), // 선택된 날짜의 스케줄
    // --- 달력 그리드 데이터 추가 ---
    val datesInMonth: List<LocalDate?> = emptyList(), // 해당 월의 날짜 리스트 (null은 빈 칸)
    val firstDayOffset: Int = 0, // 월의 첫 날짜 시작 요일 offset (0=월요일... 6=일요일) -> 여기서는 0=일요일 기준으로 변경
    // --- 달력 그리드 데이터 추가 ---
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // 일정이 있는 날짜들의 집합 (캘린더 그리드에 표시기를 위해)
    val datesWithSchedules: Set<LocalDate> = emptySet()
) {
    // 현재 연/월 표시용 포맷터
    val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
    val selectedDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
}

// 캘린더 이벤트
sealed class CalendarEvent {
    object ShowAddScheduleDialog : CalendarEvent() // 일정 추가 다이얼로그 (또는 화면)
    data class NavigateToScheduleDetail(val scheduleId: String) : CalendarEvent()
    data class ShowSnackbar(val message: String) : CalendarEvent()
}



@HiltViewModel
class CalendarViewModel @Inject constructor(
    // TODO: private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CalendarEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        updateCalendarData(YearMonth.now())
        // 초기 데이터 로드 (오늘 날짜 기준)
        loadSchedulesForDate(uiState.value.selectedDate)
        // 현재 월의 일정 요약 데이터 로드 (점 표시용)
        loadScheduleSummaryForMonth(uiState.value.currentYearMonth)
    }

    // Combined function to update month and calendar grid data
    private fun updateCalendarData(yearMonth: YearMonth, newSelectedDate: LocalDate? = null) {
        val firstDayOfMonth = yearMonth.atDay(1)
        val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = yearMonth.lengthOfMonth()
        val dates = mutableListOf<LocalDate?>()
        repeat(firstDayOfWeekValue) { dates.add(null) }
        (1..daysInMonth).forEach { dates.add(yearMonth.atDay(it)) }

        // Determine selected date: Use newSelectedDate if provided and in the new month,
        // otherwise try to keep the current selected date's day in the new month,
        // otherwise default to the 1st of the new month.
        val currentSelectedDate = _uiState.value.selectedDate
        val finalSelectedDate = newSelectedDate?.takeIf { YearMonth.from(it) == yearMonth }
            ?: yearMonth.atDay(currentSelectedDate.dayOfMonth.coerceAtMost(daysInMonth))

        // Update state, preserving existing schedules until new ones load
        _uiState.update {
            it.copy(
                currentYearMonth = yearMonth,
                datesInMonth = dates,
                selectedDate = finalSelectedDate,
                // *** Do NOT reset schedulesForSelectedDate here ***
                isLoading = true, // Set loading true for subsequent schedule load
                errorMessage = null
            )
        }
    }

    // 이전 달 클릭
    fun onPreviousMonthClick() {
        val prevMonth = _uiState.value.currentYearMonth.minusMonths(1)
        updateCalendarData(prevMonth)
        loadSchedulesForDate(_uiState.value.selectedDate) // 선택된 날짜 스케줄 로드 (선택 날짜 유지됨)
        loadScheduleSummaryForMonth(prevMonth) // 이전 달의 일정 요약 데이터 로드
    }

    // 다음 달 클릭
    fun onNextMonthClick() {
        val nextMonth = _uiState.value.currentYearMonth.plusMonths(1)
        updateCalendarData(nextMonth)
        loadSchedulesForDate(_uiState.value.selectedDate)
        loadScheduleSummaryForMonth(nextMonth) // 다음 달의 일정 요약 데이터 로드
    }

    // 날짜 선택
    fun onDateSelected(date: LocalDate) {
        // 선택된 날짜가 현재 표시된 월과 다르면 월을 변경
        val selectedYearMonth = YearMonth.from(date)
        if (selectedYearMonth != _uiState.value.currentYearMonth) {
            _uiState.update { generateCalendarState(selectedYearMonth, date) }
        } else {
            // 같은 월 내에서 날짜만 변경
            _uiState.update { it.copy(selectedDate = date) }
        }
        loadSchedulesForDate(date)
    }

    // 특정 날짜의 스케줄 로드
    private fun loadSchedulesForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: 스케줄 로드 시도 - $date")
            delay(500)
            val success = true
            if (success) {
                // --- 샘플 데이터 생성 로직 ---
                val schedules = when (date.dayOfMonth % 4) { // 4로 나눈 나머지 사용 (다양성 증가)
                    0 -> listOf(
                        ScheduleItem(
                            id = "schedule_${date}_1",
                            title = "팀 전체 회의",
                            date = date,
                            startTime = LocalTime.of(10, 0),
                            endTime = LocalTime.of(11, 30),
                            color = 0xFFEF5350 // Red 400
                        ),
                        ScheduleItem(
                            id = "schedule_${date}_2",
                            title = "거래처와 점심 식사",
                            date = date,
                            startTime = LocalTime.of(12, 30),
                            endTime = LocalTime.of(13, 30),
                            color = 0xFF66BB6A // Green 400
                        ),
                        ScheduleItem(
                            id = "schedule_${date}_3",
                            title = "개인 운동 (헬스장)",
                            date = date,
                            startTime = LocalTime.of(19, 0),
                            endTime = LocalTime.of(20, 0),
                            color = 0xFFFFA726 // Orange 400
                        )
                    )
                    1 -> listOf(
                        ScheduleItem(
                            id = "schedule_${date}_4",
                            title = "프로젝트 A 기획안 작성",
                            date = date,
                            startTime = null, // 시간 미지정 (종일)
                            endTime = null,
                            color = 0xFF42A5F5 // Blue 400
                        ),
                        ScheduleItem(
                            id = "schedule_${date}_5",
                            title = "스터디 모임",
                            date = date,
                            startTime = LocalTime.of(18, 0),
                            endTime = LocalTime.of(20, 0),
                            color = 0xFFAB47BC // Purple 400
                        )
                    )
                    2 -> listOf(
                        ScheduleItem(
                            id = "schedule_${date}_6",
                            title = "병원 예약 (치과)",
                            date = date,
                            startTime = LocalTime.of(14, 30),
                            endTime = LocalTime.of(15, 0),
                            color = 0xFF78909C // Blue Grey 400
                        )
                    )
                    else -> emptyList() // 일정이 없는 날
                }
                _uiState.update { it.copy(schedulesForSelectedDate = schedules, isLoading = false) }
                
                // 일정이 로드된 후, 현재 달의 일정 마커를 업데이트 (샘플 데이터의 일관성 유지)
                refreshScheduleMarkers()
            } else {
                _uiState.update { it.copy(schedulesForSelectedDate = emptyList(), errorMessage = "스케줄 로드 실패", isLoading = false) }
            }
        }
    }

    // 달력 상태 생성 함수
    private fun generateCalendarState(yearMonth: YearMonth, selectedDate: LocalDate? = null): CalendarUiState {
        val firstDayOfMonth = yearMonth.atDay(1)
        // 주의 시작을 일요일로 계산 (DayOfWeek.SUNDAY.value는 7)
        val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value % 7 // 일요일(7) -> 0, 월요일(1) -> 1, ... 토요일(6) -> 6
        val daysInMonth = yearMonth.lengthOfMonth()

        val dates = mutableListOf<LocalDate?>()
        // 첫 날 앞의 빈 칸 추가
        repeat(firstDayOfWeekValue) { dates.add(null) }
        // 해당 월의 날짜 추가
        for (day in 1..daysInMonth) {
            dates.add(yearMonth.atDay(day))
        }

        // 현재 선택된 날짜가 새 달에도 유효한지 확인, 아니면 새 달의 1일 선택
        val finalSelectedDate = selectedDate?.takeIf { YearMonth.from(it) == yearMonth } ?: yearMonth.atDay(1)


        return CalendarUiState(
            currentYearMonth = yearMonth,
            selectedDate = finalSelectedDate, // 이전 선택 유지 또는 1일
            datesInMonth = dates,
            firstDayOffset = firstDayOfWeekValue // 이제 사용 안 함 (리스트에 null 포함)
            // schedulesForSelectedDate는 loadSchedulesForDate에서 업데이트됨
        )
    }


    // 스케줄 아이템 클릭 시
    fun onScheduleClick(scheduleId: String) {
        viewModelScope.launch {
            println("ViewModel: 스케줄 클릭 - $scheduleId")
            _eventFlow.emit(CalendarEvent.NavigateToScheduleDetail(scheduleId))
        }
    }

    // 일정 추가 버튼 클릭 시 (예시)
    fun onAddScheduleClick() {
        viewModelScope.launch {
            println("ViewModel: 일정 추가 버튼 클릭")
            _eventFlow.emit(CalendarEvent.ShowAddScheduleDialog)
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // 특정 월에 일정이 있는 날짜들의 요약 데이터 로드
    private fun loadScheduleSummaryForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            println("ViewModel: 월간 일정 요약 로드 시도 - $yearMonth")
            // 실제 구현에서는 Repository에서 데이터를 가져와야 함
            // 샘플 구현에서는 정해진 패턴으로 일정 표시 마커를 생성
            refreshScheduleMarkers()
        }
    }

    // 일정 표시 마커 새로고침 (샘플 데이터와 일관성 유지)
    private fun refreshScheduleMarkers() {
        val currentYearMonth = _uiState.value.currentYearMonth
        val datesWithSchedules = mutableSetOf<LocalDate>()
        
        // 현재 달의 모든 날짜 확인
        val daysInMonth = currentYearMonth.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = currentYearMonth.atDay(day)
            
            // 샘플 데이터 로직과 동일한 패턴 사용
            val hasSchedule = when (day % 4) {
                0, 1, 2 -> true // 4로 나눈 나머지가 0, 1, 2인 날에는 일정 있음
                else -> false   // 나머지 날에는 일정 없음
            }
            
            if (hasSchedule) {
                datesWithSchedules.add(date)
            }
        }
        
        // UI 상태 업데이트
        _uiState.update { it.copy(datesWithSchedules = datesWithSchedules) }
    }
}
