package com.example.feature_calendar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.NavDestination
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.base.Schedule
import com.example.domain.usecase.schedule.GetScheduleSummaryForMonthUseCase
import com.example.domain.usecase.schedule.GetSchedulesForDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

// 캘린더 UI 상태
data class CalendarUiState(
    val currentYearMonth: YearMonth = YearMonth.now(), // 현재 표시 중인 연/월
    val selectedDate: LocalDate = LocalDate.now(), // 현재 선택된 날짜
    val schedulesForSelectedDate: List<Schedule> = emptyList(), // *** Updated to use domain model ***
    // --- 달력 그리드 데이터 추가 ---
    val datesInMonth: List<LocalDate?> = emptyList(), // 해당 월의 날짜 리스트 (null은 빈 칸)
    val firstDayOffset: Int = 0, // 월의 첫 날짜 시작 요일 offset (0=일요일)
    // --- 달력 그리드 데이터 추가 ---
    val isLoading: Boolean = false, // 로딩 상태 (주로 날짜별 스케줄 로딩 시)
    val isSummaryLoading: Boolean = false, // 월별 요약 로딩 상태 (선택적)
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

/**
 * 캘린더 화면의 ViewModel.
 * 캘린더 UI 상태 관리, 사용자 입력 처리, 일정 데이터 로딩 등을 담당합니다.
 *
 * @property getSchedulesForDateUseCase 특정 날짜의 일정을 가져오는 유스케이스
 * @property getScheduleSummaryForMonthUseCase 특정 월의 일정 요약 정보를 가져오는 유스케이스
 * @property navigationManger 네비게이션을 처리하는 핸들러
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getSchedulesForDateUseCase: GetSchedulesForDateUseCase,
    private val getScheduleSummaryForMonthUseCase: GetScheduleSummaryForMonthUseCase,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CalendarEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // Initialize with current month data
        updateCalendarGridData(YearMonth.now())
        // Load initial data (today's schedules and current month summary)
        loadSchedulesForDate(uiState.value.selectedDate)
        loadScheduleSummaryForMonth(uiState.value.currentYearMonth)
    }

    // Update calendar grid structure for the given month
    private fun updateCalendarGridData(yearMonth: YearMonth) {
        val firstDayOfMonth = yearMonth.atDay(1)
        // Calculate offset based on Sunday (value 7) being the first day (index 0)
        val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = yearMonth.lengthOfMonth()
        val dates = mutableListOf<LocalDate?>().apply {
            repeat(firstDayOfWeekValue) { add(null) } // Add leading nulls
            (1..daysInMonth).forEach { add(yearMonth.atDay(it)) } // Add actual dates
        }

        _uiState.update {
            it.copy(
                currentYearMonth = yearMonth,
                datesInMonth = dates,
                firstDayOffset = firstDayOfWeekValue // Store offset if needed by UI
                // Selected date is NOT changed here
            )
        }
    }

    // 이전 달 클릭
    fun onPreviousMonthClick() {
        val prevMonth = _uiState.value.currentYearMonth.minusMonths(1)
        updateCalendarGridData(prevMonth)
        // Try to select the same day in the previous month, fallback to last day
        val currentDay = _uiState.value.selectedDate.dayOfMonth
        val newSelectedDate = prevMonth.atDay(currentDay.coerceAtMost(prevMonth.lengthOfMonth()))
        selectDateInternal(newSelectedDate) // Update selection and load data
        loadScheduleSummaryForMonth(prevMonth) // Load summary for the new month
    }

    // 다음 달 클릭
    fun onNextMonthClick() {
        val nextMonth = _uiState.value.currentYearMonth.plusMonths(1)
        updateCalendarGridData(nextMonth)
        // Try to select the same day in the next month, fallback to last day
        val currentDay = _uiState.value.selectedDate.dayOfMonth
        val newSelectedDate = nextMonth.atDay(currentDay.coerceAtMost(nextMonth.lengthOfMonth()))
        selectDateInternal(newSelectedDate) // Update selection and load data
        loadScheduleSummaryForMonth(nextMonth) // Load summary for the new month
    }

    // 날짜 선택 (Public function called by UI)
    fun onDateSelected(date: LocalDate) {
        // If the selected date is in a different month, update the grid first
        if (YearMonth.from(date) != _uiState.value.currentYearMonth) {
            updateCalendarGridData(YearMonth.from(date))
            loadScheduleSummaryForMonth(YearMonth.from(date))
        }
        selectDateInternal(date)
    }

    // Internal function to update selected date state and load schedules
    private fun selectDateInternal(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSchedulesForDate(date)
    }

    /**
     * 특정 날짜의 일정을 로드하여 UI 상태를 업데이트합니다.
     *
     * @param date 일정을 로드할 날짜
     */
    private fun loadSchedulesForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Set loading, clear error
            try {
                val result = getSchedulesForDateUseCase(date).first()
                Log.d("loadScheduleForDate",result.toString())
                result.onSuccess { schedules ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            schedulesForSelectedDate = schedules
                        )
                    }
                }.onFailure { exception ->
                    viewModelScope.launch {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                schedulesForSelectedDate = emptyList(),
                                errorMessage = exception.message ?: "일정 로드 중 오류가 발생했습니다."
                            )
                        }
                        _eventFlow.emit(CalendarEvent.ShowSnackbar("일정 로드 실패: ${exception.localizedMessage}"))
                    }
                }
            } catch (e: Exception) {
                // Catch unexpected exceptions during the flow or repository call
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        schedulesForSelectedDate = emptyList(),
                        errorMessage = e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
                _eventFlow.emit(CalendarEvent.ShowSnackbar("오류 발생: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 특정 월의 일정 요약 정보(일정이 있는 날짜)를 로드하여 UI 상태를 업데이트합니다.
     *
     * @param yearMonth 일정 요약을 로드할 연월
     */
    private fun loadScheduleSummaryForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSummaryLoading = true) } // Optional: Separate loading state for summary
            try {
                val result = getScheduleSummaryForMonthUseCase(yearMonth)
                result.onSuccess { datesSet -> // datesSet is now directly Set<LocalDate>
                    _uiState.update { state ->
                        state.copy(
                            isSummaryLoading = false,
                            datesWithSchedules = datesSet
                        )
                    }
                }.onFailure { exception ->
                    // Log error or show a subtle indicator, snackbar might be too intrusive
                    println("월별 요약 로드 실패: ${exception.message}")
                    _uiState.update { state ->
                        state.copy(
                            isSummaryLoading = false,
                            datesWithSchedules = emptySet()
                        )
                    }
                    // Optional: _eventFlow.emit(CalendarEvent.ShowSnackbar("월별 요약 로드 실패"))
                }
            } catch (e: Exception) {
                println("월별 요약 로드 중 예외 발생: ${e.message}")
                 _uiState.update { state ->
                    state.copy(
                        isSummaryLoading = false,
                        datesWithSchedules = emptySet()
                    )
                }
            }
        }
    }

    // --- Event Handlers ---

    /**
     * 스케줄 아이템 클릭 시 호출됩니다.
     * 상세 화면으로 네비게이션 이벤트를 발생시킵니다.
     * @param scheduleId 선택된 일정의 ID
     */
    fun onScheduleClick(scheduleId: String) {
        navigationManger.navigate(
            NavigationCommand.NavigateToRoute(
                NavDestination.fromRoute(AppRoutes.Main.Calendar.scheduleDetail(scheduleId))
            )
        )
    }

    /**
     * FAB 클릭 시 호출됩니다.
     * 일정 추가 화면/다이얼로그 표시 이벤트를 발생시킵니다.
     */
    fun onAddScheduleClick() {
        viewModelScope.launch {
            _eventFlow.emit(CalendarEvent.ShowAddScheduleDialog)
        }
    }

    /**
     * 24시간 보기 버튼 클릭 시 호출됩니다.
     * 24시간 보기 화면으로 이동합니다.
     * 
     * @param date 표시할 날짜
     */
    fun onDate24HourClick(date: LocalDate) {
        navigationManger.navigate(
            NavigationCommand.NavigateToRoute(
                NavDestination.fromRoute(AppRoutes.Main.Calendar.calendar24Hour(
                    date.year,
                    date.monthValue,
                    date.dayOfMonth
                ))
            )
        )
    }

    /**
     * 현재 선택된 날짜의 일정 데이터를 새로 갱신합니다.
     * 다른 화면에서 돌아왔을 때 변경된 데이터를 반영하기 위해 사용됩니다.
     */
    fun refreshSchedules() {
        val currentSelectedDate = _uiState.value.selectedDate
        loadSchedulesForDate(currentSelectedDate)
        loadScheduleSummaryForMonth(YearMonth.from(currentSelectedDate))
    }

    /**
     * UI에 표시된 에러 메시지를 해제합니다.
     */
    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 일정 추가 화면으로 이동합니다.
     * 선택된 날짜를 기준으로 일정 추가 화면으로 네비게이션합니다.
     */
    fun onAddSchedule() {
        navigationManger.navigate(
            NavigationCommand.NavigateToRoute(
                NavDestination.fromRoute(AppRoutes.Main.Calendar.addSchedule(
                    uiState.value.selectedDate.year,
                    uiState.value.selectedDate.monthValue,
                    uiState.value.selectedDate.dayOfMonth
                ))
            )
        )
    }
}
