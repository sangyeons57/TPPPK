package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.routes.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
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
    private val savedStateHandle: SavedStateHandle
    // TODO: GetScheduleDetailsUseCase, UpdateScheduleUseCase 주입
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
            // TODO: 실제 일정 상세 정보 로드 (GetScheduleDetailsUseCase 사용)
            // 성공 시: _uiState.update { it.copy(title = details.title, ..., isLoading = false) }
            // 실패 시: _uiState.update { it.copy(error = "정보 로드 실패", isLoading = false) }

            // 임시 데이터 (실제 구현 시 삭제)
            kotlinx.coroutines.delay(1000)
            _uiState.update {
                it.copy(
                    title = "기존 일정 '$scheduleId' (수정)",
                    content = "기존 내용입니다.",
                    date = LocalDate.now(),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0),
                    isLoading = false
                )
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }
    // date, time 등 변경 함수 추가

    fun onSaveClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val currentState = _uiState.value
            // TODO: 유효성 검사
            // TODO: 실제 일정 업데이트 로직 (UpdateScheduleUseCase 사용)
            // val result = updateScheduleUseCase(scheduleId = currentState.scheduleId!!, title = currentState.title, ...)
            // 성공 시:
            // _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            // _eventFlow.emit(EditScheduleEvent.ShowSnackbar("일정이 저장되었습니다."))
            // _eventFlow.emit(EditScheduleEvent.NavigateBack) // 저장 후 뒤로가기
            // 실패 시:
            // _uiState.update { it.copy(isSaving = false, error = "저장 실패") }
            // _eventFlow.emit(EditScheduleEvent.ShowSnackbar("일정 저장에 실패했습니다."))


            // 임시 저장 로직
            kotlinx.coroutines.delay(1500)
            // _uiState.update { it.copy(isSaving = false, saveSuccess = true) } // REMOVE
            _uiState.update { it.copy(isSaving = false) } // ADDED - update isSaving
            _eventFlow.emit(EditScheduleEvent.ShowSnackbar("일정 '$currentState.title' 저장됨 (임시)"))
            // _eventFlow.emit(EditScheduleEvent.NavigateBack) // REMOVE - Handled by new event
            _eventFlow.emit(EditScheduleEvent.SaveSuccessAndRequestBackNavigation) // ADDED
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _eventFlow.emit(EditScheduleEvent.NavigateBack)
        }
    }
} 