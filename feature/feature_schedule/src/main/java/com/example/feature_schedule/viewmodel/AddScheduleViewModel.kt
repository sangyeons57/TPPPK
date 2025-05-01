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
data class ProjectSelectionItem(
    val id: Int,
    val name: String
)

// --- UI 상태 ---
data class AddScheduleUiState(
    val selectedDate: LocalDate? = null, // 네비게이션으로 전달받은 날짜
    val availableProjects: List<ProjectSelectionItem> = emptyList(),
    val selectedProject: ProjectSelectionItem? = null,
    val scheduleTitle: String = "",
    val scheduleContent: String = "",
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val isTimeValid: Boolean = true, // 시간 유효성 (start < end)
    val titleError: String? = null, // 제목 유효성 검사 메시지
    val timeError: String? = null, // 시간 유효성 검사 메시지
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null, // 일반 오류 메시지

    val isShowStartTimePicker: Boolean = false,
    val isShowEndTimePicker: Boolean = false,
)

// --- 이벤트 ---
sealed class AddScheduleEvent {
    object NavigateBack : AddScheduleEvent()
    data class ShowSnackbar(val message: String) : AddScheduleEvent()
}

// --- Repository 인터페이스 (가상) ---
interface ProjectRepositoryForSchedule { // 이름 변경 또는 기존 Repository 확장
    suspend fun getAvailableProjects(): Result<List<ProjectSelectionItem>>
}
interface ScheduleRepository1_가상 { // 이전 ViewModel에서 사용한 것 재활용
    suspend fun addSchedule(
        projectId: Int,
        date: LocalDate,
        title: String,
        content: String?,
        startTime: LocalTime?,
        endTime: LocalTime?
    ): Result<Unit> // Result 래퍼 사용 권장
    suspend fun getSchedulesForDate(date: LocalDate): Result<List<ScheduleItem24Hour>> // Result 래퍼 사용 권장
    suspend fun deleteSchedule(scheduleId: String): Result<Unit>
}


@HiltViewModel
class AddScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val projectRepository: ProjectRepositoryForSchedule,
    // TODO: private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val year: Int? = savedStateHandle["year"]
    private val month: Int? = savedStateHandle["month"]
    private val day: Int? = savedStateHandle["day"]

    private val _uiState = MutableStateFlow(AddScheduleUiState())
    val uiState: StateFlow<AddScheduleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddScheduleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        val initialDate = if (year != null && month != null && day != null) {
            LocalDate.of(year, month, day)
        } else {
            // 날짜 정보가 없으면 오늘 날짜 또는 에러 처리
            // 여기서는 오늘 날짜로 가정, 실제로는 에러 처리 또는 뒤로가기 필요
            LocalDate.now()
        }
        _uiState.update { it.copy(selectedDate = initialDate) }
        loadAvailableProjects()
    }

    private fun loadAvailableProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("ViewModel: Loading available projects")
            // --- TODO: 실제 프로젝트 목록 로드 (projectRepository.getAvailableProjects()) ---
            delay(500) // 임시 딜레이
            val success = true
            // val result = projectRepository.getAvailableProjects()
            // --------------------------------------------------------------------
            if (success) {
                // 임시 데이터
                val projects = listOf(
                    ProjectSelectionItem(1, "개인 프로젝트"),
                    ProjectSelectionItem(2, "스터디 그룹"),
                    ProjectSelectionItem(3, "회사 업무")
                )
                _uiState.update { it.copy(isLoading = false, availableProjects = projects) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "프로젝트 목록 로드 실패") }
            }
        }
    }

    fun onProjectSelected(project: ProjectSelectionItem) {
        _uiState.update { it.copy(selectedProject = project) }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(scheduleTitle = title, titleError = null) } // 에러 초기화
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(scheduleContent = content) }
    }

    fun requestStartTimePicker(value: Boolean) {
        println("requestStartTimePicker: $value")
        _uiState.update { it.copy(isShowStartTimePicker = value) }
        //viewModelScope.launch { _eventFlow.emit(AddScheduleEvent.ShowStartTimePicker) }
    }

    fun requestEndTimePicker(value: Boolean) {
        _uiState.update { it.copy(isShowEndTimePicker = value) }
        // viewModelScope.launch { _eventFlow.emit(AddScheduleEvent.ShowEndTimePicker) }
    }

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val startTime = LocalTime.of(hour, minute)
        _uiState.update { it.copy(startTime = startTime, timeError = null) } // 에러 초기화
        validateTimeRange()
    }

    fun onEndTimeSelected(hour: Int, minute: Int) {
        val endTime = LocalTime.of(hour, minute)
        _uiState.update { it.copy(endTime = endTime, timeError = null) } // 에러 초기화
        validateTimeRange()
    }

    private fun validateTimeRange() {
        val start = _uiState.value.startTime
        val end = _uiState.value.endTime
        val isValid = if (start != null && end != null) {
            !end.isBefore(start) && start != end // 종료시간이 시작시간보다 이전이 아니고, 같지도 않음
        } else {
            true // 하나라도 null이면 유효하다고 간주 (저장 시 다시 확인)
        }
        val errorMsg = if (!isValid) "종료 시간은 시작 시간 이후여야 합니다." else null
        _uiState.update { it.copy(isTimeValid = isValid, timeError = errorMsg) }
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        val date = currentState.selectedDate
        val project = currentState.selectedProject
        val title = currentState.scheduleTitle.trim()
        val start = currentState.startTime
        val end = currentState.endTime

        // 필수 입력 값 유효성 검사
        var hasError = false
        if (project == null) {
            _uiState.update { it.copy(errorMessage = "프로젝트를 선택해주세요.") } // 일반 에러로 표시
            hasError = true
        }
        if (title.isBlank()) {
            _uiState.update { it.copy(titleError = "일정 제목을 입력해주세요.") }
            hasError = true
        }
        if (start == null || end == null) {
            _uiState.update { it.copy(timeError = "시간을 설정해주세요.") }
            hasError = true
        } else if (!currentState.isTimeValid) {
            // validateTimeRange에서 이미 timeError가 설정됨
            hasError = true
        }

        if (hasError || date == null) {
            viewModelScope.launch { _eventFlow.emit(AddScheduleEvent.ShowSnackbar("입력값을 확인해주세요.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: Saving schedule...")
            // --- TODO: 실제 저장 로직 (scheduleRepository.addSchedule(...)) ---
            delay(1000)
            val success = true
            // val result = scheduleRepository.addSchedule(
            //     projectId = project.id,
            //     date = date,
            //     title = title,
            //     content = currentState.scheduleContent.trim().takeIf { it.isNotEmpty() }, // 내용 없으면 null
            //     startTime = start,
            //     endTime = end
            // )
            // -------------------------------------------------------------
            if (success) {
                _eventFlow.emit(AddScheduleEvent.ShowSnackbar("일정이 추가되었습니다."))
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "일정 저장 실패") }
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}