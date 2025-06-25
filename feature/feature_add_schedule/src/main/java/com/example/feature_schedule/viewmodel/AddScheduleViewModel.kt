package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.enum.ScheduleStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.schedule.ScheduleUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

// --- 데이터 모델 ---
data class ProjectSelectionItem(
    val id: DocumentId,
    val name: ProjectName
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
    val errorMessage: String? = null, // 일반 오류 메시지

    val isShowStartTimePicker: Boolean = false,
    val isShowEndTimePicker: Boolean = false,
)

// --- 이벤트 ---
sealed class AddScheduleEvent {
    data class ShowSnackbar(val message: String) : AddScheduleEvent()
}

@HiltViewModel
class AddScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleUseCaseProvider: ScheduleUseCaseProvider,
    private val projectUseCaseProvider: CoreProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val year: Int? = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_YEAR)
    private val month: Int? = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_MONTH)
    private val day: Int? = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_DAY)

    private val _uiState = MutableStateFlow(AddScheduleUiState())
    val uiState: StateFlow<AddScheduleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddScheduleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    val scheduleUseCases = scheduleUseCaseProvider.createForCurrentUser()
    val projectUseCases = projectUseCaseProvider.createForCurrentUser()
    private val addScheduleUseCase = scheduleUseCases.addScheduleUseCase
    private val getUserParticipatingProjectsUseCase = projectUseCases.getUserParticipatingProjectsUseCase

    init {
        val initialDate = if (year != null && month != null && day != null) {
            LocalDate.of(year, month, day)
        } else {
            // 날짜 정보가 없으면 오늘 날짜로 설정
            LocalDate.now()
        }
        _uiState.update { it.copy(selectedDate = initialDate) }
        loadAvailableProjects()
    }

    /**
     * 일정 생성이 가능한 프로젝트 목록을 로드합니다.
     * 기본적으로 개인 일정 옵션을 항상 포함하며, 그 외 사용자가 참여 중인 프로젝트를 불러옵니다.
     */
    private fun loadAvailableProjects() {
        viewModelScope.launch {
            // 개인 일정 옵션 기본 추가
            val personalScheduleOption = ProjectSelectionItem(
                id = DocumentId.PERSONAL_SCHEDULE_PROJECT_ID,
                name = ProjectName("개인 일정")
            )
            _uiState.update { it.copy(isLoading = true, availableProjects = listOf(personalScheduleOption), selectedProject = personalScheduleOption) }

            getUserParticipatingProjectsUseCase().collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val fetchedProjects = result.data.map { project ->
                            ProjectSelectionItem(
                                id = project.id,
                                name = project.name
                            )
                        }

                        // 개인 일정 옵션과 불러온 프로젝트 목록 합치기
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                availableProjects = listOf(personalScheduleOption) + fetchedProjects
                            )
                        }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "프로젝트 목록을 불러오는데 실패했습니다: ${result.error.message ?: "알 수 없는 오류"}"
                            ) 
                        }
                        _eventFlow.emit(AddScheduleEvent.ShowSnackbar("프로젝트 목록을 불러오는데 실패했습니다."))
                    }
                    else -> {
                        // 로딩 상태 등 무시
                    }
                }
            }
        }
    }

    fun onProjectSelected(project: ProjectSelectionItem) {
        if (project.id == DocumentId.PERSONAL_SCHEDULE_PROJECT_ID) {
            // "개인 일정" 선택 시 selectedProject를 null로 설정
            _uiState.update { it.copy(selectedProject = null) }
        } else {
            _uiState.update { it.copy(selectedProject = project) }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(scheduleTitle = title, titleError = null) } // 에러 초기화
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(scheduleContent = content) }
    }

    fun requestStartTimePicker(value: Boolean) {
        _uiState.update { it.copy(isShowStartTimePicker = value) }
    }

    fun requestEndTimePicker(value: Boolean) {
        _uiState.update { it.copy(isShowEndTimePicker = value) }
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
        val content = currentState.scheduleContent.trim()
        val start = currentState.startTime
        val end = currentState.endTime

        var hasError = false
        if (title.isBlank()) {
            _uiState.update { it.copy(titleError = "일정 제목을 입력해주세요.") }
            hasError = true
        }
        if (start == null || end == null) {
            _uiState.update { it.copy(timeError = "시간을 설정해주세요.") }
            hasError = true
        } else if (!currentState.isTimeValid) {
            hasError = true
        }

        if (hasError || date == null) {
            viewModelScope.launch { _eventFlow.emit(AddScheduleEvent.ShowSnackbar("입력값을 확인해주세요.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val localStartTime = LocalDateTime.of(date, start!!)
            val localEndTime = LocalDateTime.of(date, end!!)
            val instantStartTime = DateTimeUtil.toInstant(localStartTime)
            val instantEndTime = DateTimeUtil.toInstant(localEndTime)

            val result = addScheduleUseCase(
                title = title,
                content = content,
                startTime = instantStartTime,
                endTime = instantEndTime,
                status = ScheduleStatus.CONFIRMED, // Default for new schedules
                color = null, // Color selection UI not implemented yet
                projectId = project?.id.takeIf { it != DocumentId.PERSONAL_SCHEDULE_PROJECT_ID }?.value
            )

            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(AddScheduleEvent.ShowSnackbar("일정이 추가되었습니다."))
                    _uiState.update { it.copy(isLoading = false) }
                    navigationManger.navigateBack()
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "일정 저장 실패: ${result.error.message ?: "알 수 없는 오류"}"
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "알 수 없는 오류"
                        )
                    }
                }
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun navigateBack() {
        navigationManger.navigateBack()
    }
}