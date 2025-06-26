package com.example.feature_schedule_detail.viewmodel


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.NavigationResultKeys
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.domain.provider.schedule.ScheduleUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 데이터 모델 ---
data class ScheduleDetailItem(
    val id: DocumentId,
    val title: ScheduleTitle,
    val projectName: String?, // 프로젝트 이름 (없을 수도 있음)
    val date: String, // 포맷된 날짜 문자열 (예: "2025년 4월 15일 (화)")
    val time: String, // 포맷된 시간 범위 문자열 (예: "오후 2:00 ~ 오후 3:30", "하루 종일")
    val content: ScheduleContent // 일정 내용
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
    object ShowDeleteConfirmDialog : ScheduleDetailEvent()
    data class ShowSnackbar(val message: String) : ScheduleDetailEvent()
}

@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleUseCaseProvider: ScheduleUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val scheduleUseCases = scheduleUseCaseProvider.createForCurrentUser()

    private val _uiState = MutableStateFlow(ScheduleDetailUiState(isLoading = true))
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ScheduleDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            val scheduleId = savedStateHandle.getRequiredString(RouteArgs.SCHEDULE_ID)

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
                val result = scheduleUseCases.getScheduleDetailUseCase(scheduleId)
                result.onSuccess { schedule ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scheduleDetail = schedule.toDetailItem() // Map domain to UI item
                        )
                    }
                }.onFailure { exception ->
                    viewModelScope.launch {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "일정 정보를 불러오지 못했습니다."
                            )
                        }
                         _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("정보 로드 실패: ${exception.localizedMessage}"))
                    }
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
        "yyyy년 M월 d일 (E)" // Define pattern locally or in DateTimeUtil if widely used

        // null 체크 추가 및 DateTimeUtil 메서드 수정
        val dateString = this.startTime.let { DateTimeUtil.formatDate(DateTimeUtil.toLocalDateTime(it)) }

        // 시간 포맷팅 - null 체크 추가
        val start = this.startTime.let { DateTimeUtil.formatTime(DateTimeUtil.toLocalDateTime(it)) }
        val end = this.endTime.let { DateTimeUtil.formatTime(DateTimeUtil.toLocalDateTime(it)) }
        val timeString = "$start ~ $end"

        val projectName = if (this.projectId?.value.isNullOrEmpty()) null else "Project ${this.projectId}"

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
        val scheduleId = uiState.value.scheduleId ?: return
        navigationManger.navigateToEditSchedule(scheduleId)
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
                val result = scheduleUseCases.deleteScheduleUseCase(uiState.value.scheduleId ?: "")
                result.onSuccess {
                    viewModelScope.launch {
                        _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("일정이 삭제되었습니다."))
                        _uiState.update { it.copy(isLoading = false, deleteSuccess = true) }
                        // 스케줄 삭제 성공을 캘린더 화면에 알림
                        navigationManger.setResult(NavigationResultKeys.REFRESH_SCHEDULE_LIST, true)
                        navigationManger.navigateBack() // 삭제 성공 후 뒤로가기
                    }
                }.onFailure { exception ->
                    viewModelScope.launch {
                         _uiState.update { it.copy(isLoading = false, error = "일정 삭제 실패: ${exception.message}") }
                         _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("일정 삭제 실패: ${exception.localizedMessage}"))
                     }
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

    /**
     * 뒤로가기 네비게이션 처리
     */
    fun navigateBack() {
        navigationManger.navigateBack()
    }

}