package com.example.feature_schedule.viewmodel


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.Schedule
import com.example.domain.usecase.schedule.DeleteScheduleUseCase
import com.example.domain.usecase.schedule.GetScheduleDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val deleteSuccess: Boolean = false, // 삭제 성공 시 네비게이션 트리거
    val scheduleId: String? = null
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
    private val getScheduleDetailUseCase: GetScheduleDetailUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleDetailUiState(isLoading = true))
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ScheduleDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            val scheduleId = savedStateHandle.get<String>(AppRoutes.Main.Calendar.ARG_SCHEDULE_ID)
            if (scheduleId == null) {
                _uiState.update { it.copy(isLoading = false, error = "일정 ID를 찾을 수 없습니다.") }
                return@launch
            }
            // _uiState.update { it.copy(scheduleId = scheduleId, isLoading = true) } // Moved to loadScheduleDetails
            loadScheduleDetails(scheduleId)
        }
    }

    // ADDED: Function to reload schedule details
    fun refreshScheduleDetails() {
        viewModelScope.launch {
            val scheduleId = uiState.value.scheduleId // Get current scheduleId from state
            if (scheduleId == null) {
                _uiState.update { it.copy(isLoading = false, error = "일정 ID를 찾을 수 없습니다. (새로고침 실패)") }
                return@launch
            }
            loadScheduleDetails(scheduleId) // Call the existing loading logic
        }
    }

    // Modified to be callable for refresh
    private fun loadScheduleDetails(scheduleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(scheduleId = scheduleId, isLoading = true, error = null) } // Set loading and clear previous error
            try {
                val result = getScheduleDetailUseCase(scheduleId)
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
        val datePattern = "yyyy년 M월 d일 (E)" // Define pattern locally or in DateTimeUtil if widely used

        val dateString = DateTimeUtil.format(this.startTime, datePattern)

        val start = DateTimeUtil.formatChatTime(this.startTime)
        val end = DateTimeUtil.formatChatTime(this.endTime)
        val timeString = "$start ~ $end"

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
            _eventFlow.emit(ScheduleDetailEvent.NavigateToEditSchedule(uiState.value.scheduleId ?: ""))
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
                val result = deleteScheduleUseCase(uiState.value.scheduleId ?: "")
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