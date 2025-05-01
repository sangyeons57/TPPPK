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
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// --- 데이터 모델 ---
data class ScheduleDetailItem(
    val id: String,
    val title: String,
    val projectName: String?, // 프로젝트 이름 (없을 수도 있음)
    val date: String, // 포맷된 날짜 문자열 (예: "2025년 4월 15일 (화)")
    val time: String, // 포맷된 시간 범위 문자열 (예: "오후 2:00 ~ 오후 3:30")
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

// --- Repository 인터페이스 (가상) ---
interface ScheduleRepository { // 이전 ViewModel에서 사용한 것 확장 또는 신규
    suspend fun getScheduleDetail(scheduleId: String): Result<ScheduleDetailItem> // 상세 정보 반환
    suspend fun deleteSchedule(scheduleId: String): Result<Unit>
    // ... 기존 함수들 ...
}

@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val scheduleId: String = savedStateHandle["scheduleId"] ?: error("scheduleId가 전달되지 않았습니다.")

    private val _uiState = MutableStateFlow(ScheduleDetailUiState(isLoading = true))
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ScheduleDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadScheduleDetail(scheduleId)
    }

    private fun loadScheduleDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading schedule detail for ID $id")
            // --- TODO: 실제 데이터 로딩 (scheduleRepository.getScheduleDetail(id)) ---
            delay(700) // 임시 딜레이
            val success = true
            // val result = scheduleRepository.getScheduleDetail(id)
            // ------------------------------------------------------------------
            if (success) {
                // 임시 데이터 (실제로는 Repository 결과 사용)
                val detail = ScheduleDetailItem(
                    id = id,
                    title = "상세 일정 제목 $id",
                    projectName = if (id.toIntOrNull()?.rem(2) == 0) "연관 프로젝트 ${id}" else null,
                    date = LocalDate.now().plusDays(id.toLongOrNull() ?: 0)
                        .format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)),
                    time = "${LocalTime.now().minusHours(1).format(DateTimeFormatter.ofPattern("a h:mm"))} ~ ${LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm"))}",
                    content = "이것은 일정 ID $id 에 대한 상세 설명입니다.\n여러 줄의 내용이 여기에 표시될 수 있습니다."
                )
                _uiState.update { it.copy(isLoading = false, scheduleDetail = detail) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "일정 정보를 불러오지 못했습니다.") }
            }
        }
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
            println("ViewModel: Deleting schedule ID $scheduleId")
            // --- TODO: 실제 삭제 로직 (scheduleRepository.deleteSchedule(scheduleId)) ---
            delay(500)
            val success = true
            // val result = scheduleRepository.deleteSchedule(scheduleId)
            // -----------------------------------------------------------------
            if (success) {
                _eventFlow.emit(ScheduleDetailEvent.ShowSnackbar("일정이 삭제되었습니다."))
                _uiState.update { it.copy(isLoading = false, deleteSuccess = true) } // 삭제 성공 및 네비게이션 트리거
            } else {
                _uiState.update { it.copy(isLoading = false, error = "일정 삭제 실패") }
            }
        }
    }

}