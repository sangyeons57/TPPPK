package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil // Added
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.Schedule // Added
import com.example.domain.usecase.schedule.GetScheduleDetailUseCase // Added
import com.example.domain.usecase.schedule.UpdateScheduleUseCase // Added
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant // Added
import java.time.LocalDate
import java.time.LocalDateTime // Added
import java.time.LocalTime
// import java.time.ZoneId // ZoneId might not be directly needed here if DateTimeUtil handles it
import javax.inject.Inject

// 일정 수정 화면 UI 상태
data class EditScheduleUiState(
    val scheduleId: String? = null,
    val title: String = "",
    val content: String? = null,
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isShowStartTimePicker: Boolean = false, // 시간 선택 다이얼로그 표시 여부
    val isShowEndTimePicker: Boolean = false, // 시간 선택 다이얼로그 표시 여부
    // val saveSuccess: Boolean = false // REMOVE
    // 프로젝트 선택, 알림 설정 등 추가 필드
)

// 일정 수정 화면 이벤트
sealed class EditScheduleEvent {
    object NavigateBack : EditScheduleEvent()
    object SaveSuccessAndRequestBackNavigation : EditScheduleEvent() // ADDED
    data class ShowSnackbar(val message: String) : EditScheduleEvent()
}

/**
 * 기존 일정 수정 로직을 관리하는 ViewModel.
 */
@HiltViewModel
class EditScheduleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getScheduleDetailUseCase: GetScheduleDetailUseCase, // Added
    private val updateScheduleUseCase: UpdateScheduleUseCase   // Added
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditScheduleUiState())
    val uiState: StateFlow<EditScheduleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditScheduleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            val scheduleId = savedStateHandle.get<String>(AppRoutes.Main.Calendar.ARG_SCHEDULE_ID)
            if (scheduleId == null) {
                _uiState.update { it.copy(isLoading = false, error = "일정 ID를 찾을 수 없습니다.") }
                _eventFlow.emit(EditScheduleEvent.ShowSnackbar("잘못된 접근입니다."))
                // NavigateBack 처리는 Screen에서 eventFlow를 구독하여 처리 가능
                return@launch
            }
            _uiState.update { it.copy(scheduleId = scheduleId, isLoading = true) }
            loadScheduleDetails(scheduleId)
        }
    }

    private fun loadScheduleDetails(scheduleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = getScheduleDetailUseCase(scheduleId)
            result.onSuccess { schedule ->
                _uiState.update {
                    it.copy(
                        title = schedule.title,
                        content = schedule.content,
                        // Convert Instant to LocalDate and LocalTime for UI state
                        date = DateTimeUtil.toLocalDate(schedule.startTime),
                        startTime = DateTimeUtil.toLocalTime(schedule.startTime),
                        endTime = DateTimeUtil.toLocalTime(schedule.endTime),
                        // TODO: Add projectId to UiState and map it here if schedules can be associated with projects
                        isLoading = false
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "일정 정보를 불러오지 못했습니다: ${exception.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun onTimeClick() {
        _uiState.update { it.copy(isShowStartTimePicker = true) }
    }

    // date, time 등 변경 함수 추가

    fun requestStartTimePicker(show: Boolean) {
        _uiState.update { it.copy(isShowStartTimePicker = show) }
    }

    fun requestEndTimePicker(show: Boolean) {
        _uiState.update { it.copy(isShowEndTimePicker = show) }
    }

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val selectedTime = LocalTime.of(hour, minute)
        _uiState.update {
            it.copy(
                startTime = selectedTime,
                isShowStartTimePicker = false, // 시작 시간 선택 후 닫기
                isShowEndTimePicker = true    // 종료 시간 선택 열기
            )
        }
    }

    fun onEndTimeSelected(hour: Int, minute: Int) {
        val selectedTime = LocalTime.of(hour, minute)
        // 시작 시간보다 이전이거나 같으면 유효성 검사 실패 처리 (예시)
        if (_uiState.value.startTime != null && selectedTime.isBefore(_uiState.value.startTime)) {
            // TODO: 사용자에게 오류 메시지 표시 (예: Snackbar)
            viewModelScope.launch {
                _eventFlow.emit(EditScheduleEvent.ShowSnackbar("종료 시간은 시작 시간 이후여야 합니다."))
            }
            _uiState.update { it.copy(isShowEndTimePicker = false) } // 에러 발생 시에도 닫기는 해야 함
            return
        }
        _uiState.update {
            it.copy(
                endTime = selectedTime,
                isShowEndTimePicker = false // 종료 시간 선택 후 닫기
            )
        }
    }

    fun onSaveClicked() {
        val currentState = _uiState.value
        if (currentState.scheduleId == null) {
            viewModelScope.launch { _eventFlow.emit(EditScheduleEvent.ShowSnackbar("오류: 일정 ID가 없습니다.")) }
            return
        }
        if (currentState.title.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(EditScheduleEvent.ShowSnackbar("일정 제목을 입력해주세요.")) }
            return
        }
        if (currentState.date == null || currentState.startTime == null || currentState.endTime == null) {
            viewModelScope.launch { _eventFlow.emit(EditScheduleEvent.ShowSnackbar("날짜와 시간을 모두 설정해주세요.")) }
            return
        }
        if (currentState.endTime.isBefore(currentState.startTime)) {
            viewModelScope.launch { _eventFlow.emit(EditScheduleEvent.ShowSnackbar("종료 시간은 시작 시간 이후여야 합니다.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            // Convert LocalDate/LocalTime from UI back to Instant for domain model
            val startInstant = DateTimeUtil.toInstant(LocalDateTime.of(currentState.date, currentState.startTime))
            val endInstant = DateTimeUtil.toInstant(LocalDateTime.of(currentState.date, currentState.endTime))

            if (startInstant == null || endInstant == null) {
                 _uiState.update { it.copy(isSaving = false, error = "시간 변환 오류") }
                 _eventFlow.emit(EditScheduleEvent.ShowSnackbar("시간 변환 중 오류가 발생했습니다."))
                 return@launch
            }
            
            val updatedSchedule = Schedule(
                id = currentState.scheduleId,
                creatorId = "", // Per instruction for now
                projectId = null, // Per instruction for now
                title = currentState.title,
                content = currentState.content,
                startTime = startInstant,
                endTime = endInstant,
                createdAt = Instant.now() // Per instruction, or fetch original
            )

            val result = updateScheduleUseCase(updatedSchedule)
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _eventFlow.emit(EditScheduleEvent.ShowSnackbar("일정이 저장되었습니다."))
                _eventFlow.emit(EditScheduleEvent.SaveSuccessAndRequestBackNavigation)
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "저장 실패: ${exception.localizedMessage}"
                    )
                }
                _eventFlow.emit(EditScheduleEvent.ShowSnackbar("일정 저장에 실패했습니다: ${exception.localizedMessage}"))
            }
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _eventFlow.emit(EditScheduleEvent.NavigateBack)
        }
    }
} 