package com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

// --- 데이터 모델 ---
data class ScheduleItem24Hour(
    val id: String,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val color: Long // ARGB Long
)

// --- UI 상태 ---
sealed interface Calendar24HourUiState {
    object Loading : Calendar24HourUiState
    data class Success(
        val selectedDate: LocalDate?, // 초기 로딩 시 null일 수 있음
        val schedules: List<ScheduleItem24Hour> = emptyList()
    ) : Calendar24HourUiState
    data class Error(val message: String) : Calendar24HourUiState
}

// --- 이벤트 ---
sealed class Calendar24HourEvent {
    object NavigateBack : Calendar24HourEvent()
    object NavigateToAddSchedule : Calendar24HourEvent()
    data class NavigateToScheduleDetail(val scheduleId: String) : Calendar24HourEvent()
    data class ShowScheduleEditDialog(val scheduleId: String) : Calendar24HourEvent()
    data class ShowSnackbar(val message: String) : Calendar24HourEvent()
}

// --- Repository 인터페이스 (가상) ---
interface ScheduleRepository2_가상 {
    suspend fun getSchedulesForDate(date: LocalDate): Result<List<ScheduleItem24Hour>> // Result 래퍼 사용 권장
    suspend fun deleteSchedule(scheduleId: String): Result<Unit>
}

@HiltViewModel
class Calendar24HourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val scheduleRepository: ScheduleRepository // 실제 Repository 주입
) : ViewModel() {

    private val year: Int = savedStateHandle["year"] ?: LocalDate.now().year
    private val month: Int = savedStateHandle["month"] ?: LocalDate.now().monthValue
    private val day: Int = savedStateHandle["day"] ?: LocalDate.now().dayOfMonth

    private val _uiState = MutableStateFlow<Calendar24HourUiState>(Calendar24HourUiState.Loading)
    val uiState: StateFlow<Calendar24HourUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<Calendar24HourEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadSchedules(LocalDate.of(year, month, day))
    }

    private fun loadSchedules(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = Calendar24HourUiState.Loading
            println("ViewModel: Loading schedules for $date")
            // --- TODO: 실제 데이터 로딩 (scheduleRepository.getSchedulesForDate(date)) ---
            delay(1000) // 임시 딜레이
            val success = true // 임시 성공/실패
            // val result = scheduleRepository.getSchedulesForDate(date)
            // ---------------------------------------------------------------------
            if (success) {
                // 임시 샘플 데이터
                val schedules = listOf(
                    ScheduleItem24Hour("1", "팀 회의", LocalTime.of(10, 0), LocalTime.of(11, 30), 0xFFEF5350),
                    ScheduleItem24Hour("2", "점심 약속", LocalTime.of(12, 30), LocalTime.of(13, 30), 0xFF66BB6A),
                    ScheduleItem24Hour("3", "개인 프로젝트", LocalTime.of(15, 0), LocalTime.of(17, 0), 0xFF42A5F5),
                    ScheduleItem24Hour("4", "API 디자인 검토", LocalTime.of(15, 30), LocalTime.of(16, 30), 0xFFAB47BC) // 겹치는 일정 예시
                )
                _uiState.value = Calendar24HourUiState.Success(date, schedules)
            } else {
                _uiState.value = Calendar24HourUiState.Error("스케줄 로딩 실패")
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            println("ViewModel: Deleting schedule $scheduleId")
            // --- TODO: 실제 스케줄 삭제 로직 (scheduleRepository.deleteSchedule(scheduleId)) ---
            delay(300)
            val success = true
            // val result = scheduleRepository.deleteSchedule(scheduleId)
            // --------------------------------------------------------------------------
            if (success) {
                // 성공 시, 현재 상태가 Success이면 해당 스케줄 제거 후 UI 업데이트
                val currentState = _uiState.value
                if (currentState is Calendar24HourUiState.Success) {
                    _uiState.value = currentState.copy(
                        schedules = currentState.schedules.filterNot { it.id == scheduleId }
                    )
                    _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("일정이 삭제되었습니다."))
                }
            } else {
                _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("일정 삭제 실패"))
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.NavigateBack) }
    }

    fun onAddScheduleClick() {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.NavigateToAddSchedule) }
    }

    fun onScheduleClick(scheduleId: String) {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.NavigateToScheduleDetail(scheduleId)) }
    }

    fun onScheduleLongClick(scheduleId: String) {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.ShowScheduleEditDialog(scheduleId)) }
    }
}