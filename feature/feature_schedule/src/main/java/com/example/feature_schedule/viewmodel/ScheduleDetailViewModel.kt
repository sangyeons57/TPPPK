package com.example.feature_schedule.viewmodel


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Schedule
import com.example.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// --- 데이터 모델 ---
data class ScheduleDetailItem(
    val id: String,
    val title: String,
    val projectName: String?, // 프로젝트 이름 (없을 수도 있음)
    val date: String, // 포맷된 날짜 문자열 (예: "2025년 4월 15일 (화)")
    val time: String, // 포맷된 시간 범위 문자열 (예: "오후 2:00 ~ 오후 3:30", "하루 종일")
    val content: String? // 일정 내용
)

// --- UI 상태 ---
data class ScheduleDetailUiState(
    val isLoading: Boolean = false,
    val scheduleDetail: ScheduleDetailItem? = null,
    val error: String? = null,
    val deleteSuccess: Boolean = false // 삭제 성공 시 네비게이션 트리거
)

// --- 이벤트 ---
sealed class ScheduleDetailEvent {
    object NavigateBack : ScheduleDetailEvent()
    data class NavigateToEditSchedule(val scheduleId: String) : ScheduleDetailEvent()
    object ShowDeleteConfirmDialog : ScheduleDetailEvent()
    data class ShowSnackbar(val message: String) : ScheduleDetailEvent()
}

@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val scheduleId: String = savedStateHandle["scheduleId"] ?: error("scheduleId가 전달되지 않았습니다.")

    private val _uiState = MutableStateFlow(ScheduleDetailUiState(isLoading = true))
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ScheduleDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Date and Time formatters
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)
    private val timeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

    init {
        loadScheduleDetail(scheduleId)
    }

    private fun loadScheduleDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = scheduleRepository.getScheduleDetail(id)
                result.onSuccess { schedule ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scheduleDetail = schedule.toDetailItem() // Map domain to UI item
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "일정 정보를 불러오지 못했습니다."
                        )
                    }
                     _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("정보 로드 실패: ${exception.localizedMessage}"))
                }
            } catch (e: Exception) {
                 _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
                _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("오류 발생: ${e.localizedMessage}"))
            }
        }
    }

    // Helper function to map domain Schedule to ScheduleDetailItem
    private fun Schedule.toDetailItem(): ScheduleDetailItem {
        // UTC LocalDateTime을 사용자의 로컬 시간대로 변환
        val localStartTime = this.startTime.atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        val localEndTime = this.endTime.atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()

        val dateString = localStartTime.format(dateFormatter)
        val timeString = if (this.isAllDay) {
            "하루 종일"
        } else {
            val start = localStartTime.format(timeFormatter)
            val end = localEndTime.format(timeFormatter)
            "$start ~ $end"
        }
        // TODO: Fetch project name based on this.projectId if necessary
        val projectName = this.projectId?.let { "Project $it" } // Placeholder

        return ScheduleDetailItem(
            id = this.id,
            title = this.title,
            projectName = projectName,
            date = dateString,
            time = timeString,
            content = this.content
        )
    }

    fun onEditClick() {
        viewModelScope.launch {
            // 수정 화면으로 이동하는 이벤트 발생 (scheduleId 전달)
            _eventFlow.emit(ScheduleDetailEvent.NavigateToEditSchedule(scheduleId))
        }
    }

    fun onDeleteClick() {
        viewModelScope.launch {
            // 삭제 확인 다이얼로그 표시 이벤트 발생
            _eventFlow.emit(ScheduleDetailEvent.ShowDeleteConfirmDialog)
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // 삭제 중 로딩 표시
            try {
                val result = scheduleRepository.deleteSchedule(scheduleId)
                result.onSuccess {
                    _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("일정이 삭제되었습니다."))
                    _uiState.update { it.copy(isLoading = false, deleteSuccess = true) } // 삭제 성공 및 네비게이션 트리거
                }.onFailure { exception ->
                     _uiState.update { it.copy(isLoading = false, error = "일정 삭제 실패: ${exception.message}") }
                     _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("일정 삭제 실패: ${exception.localizedMessage}"))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "일정 삭제 중 오류 발생: ${e.message}") }
                _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("오류 발생: ${e.localizedMessage}"))
            }
        }
    }

    // Add function to clear error message if needed by UI
    fun errorMessageShown() {
        _uiState.update { it.copy(error = null) }
    }

}