package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.AuthUtil
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Schedule
import com.example.domain.usecase.project.GetSchedulableProjectsUseCase
import com.example.domain.usecase.schedule.AddScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getString

// --- 데이터 모델 ---
data class ProjectSelectionItem(
    val id: String,
    val name: String
)
const val PERSONAL_SCHEDULE_PROJECT_ID = "-1" // 개인 일정을 나타내는 상수 ID

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
    object NavigateBack : AddScheduleEvent()
    object SaveSuccessAndRequestBackNavigation : AddScheduleEvent()
    data class ShowSnackbar(val message: String) : AddScheduleEvent()
}

@HiltViewModel
class AddScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSchedulableProjectsUseCase: GetSchedulableProjectsUseCase,
    private val addScheduleUseCase: AddScheduleUseCase,
) : ViewModel() {

    private val year: Int? = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_YEAR)
    private val month: Int? = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_MONTH)
    private val day: Int? = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_DAY)

    private val _uiState = MutableStateFlow(AddScheduleUiState())
    val uiState: StateFlow<AddScheduleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddScheduleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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

    private fun loadAvailableProjects() {
        viewModelScope.launch {
            // 개인 일정 옵션 기본 추가
            val personalScheduleOption = ProjectSelectionItem(id = PERSONAL_SCHEDULE_PROJECT_ID, name = "개인 일정")
            _uiState.update { it.copy(isLoading = true, availableProjects = listOf(personalScheduleOption), selectedProject = null) } // selectedProject를 null로 초기화 (개인 일정이 기본값)

            val result = getSchedulableProjectsUseCase()

            if (result.isSuccess) {
                val fetchedProjects = result.getOrThrow().map { project ->
                    ProjectSelectionItem(
                        id = project.id ?: "",
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
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "프로젝트 목록 로드 실패: ${result.exceptionOrNull()?.message ?: "알 수 없는 오류"}"
                    ) 
                }
            }
        }
    }

    fun onProjectSelected(project: ProjectSelectionItem) {
        if (project.id == PERSONAL_SCHEDULE_PROJECT_ID) {
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
        val project = currentState.selectedProject // null일 수 있음
        val title = currentState.scheduleTitle.trim()
        val content = currentState.scheduleContent.trim()
        val start = currentState.startTime
        val end = currentState.endTime

        // 필수 입력 값 유효성 검사
        var hasError = false
        // 프로젝트 선택은 필수가 아니므로 검사 제거
//        if (project == null) {
//            _uiState.update { it.copy(errorMessage = "프로젝트를 선택해주세요.") } // 일반 에러로 표시
//            hasError = true
//        }
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

            // 로컬 LocalDateTime 생성
            val localStartTime = LocalDateTime.of(date, start!!)
            val localEndTime = LocalDateTime.of(date, end!!)

            // UTC LocalDateTime으로 변환
            val instantStartTime = DateTimeUtil.toInstant(localStartTime)
            val instantEndTime = DateTimeUtil.toInstant(localEndTime)

            // 현재 사용자 ID 가져오기
            val userId = try {
                AuthUtil.getCurrentUserId()
            } catch (e: IllegalStateException) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "일정을 저장하려면 로그인이 필요합니다."
                    ) 
                }
                return@launch
            }

            // Schedule 객체 생성 (UTC 시간 사용)
            // project가 null이면 projectId도 null이 됨
            val schedule = Schedule(
                id = UUID.randomUUID().toString(),
                creatorId = userId,
                projectId = project?.id.takeIf { it != PERSONAL_SCHEDULE_PROJECT_ID },
                title = title,
                content = content.takeIf { it.isNotEmpty() }, // 내용 없으면 null
                startTime = instantStartTime!!,
                endTime = instantEndTime!!,
                createdAt = DateTimeUtil.nowInstant()
            )
            
            // Use UseCase to add the schedule
            val result = addScheduleUseCase(schedule)
            
            if (result.isSuccess) {
                _eventFlow.emit(AddScheduleEvent.ShowSnackbar("일정이 추가되었습니다."))
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(AddScheduleEvent.SaveSuccessAndRequestBackNavigation)
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "일정 저장 실패: ${result.exceptionOrNull()?.message ?: "알 수 없는 오류"}"
                    )
                }
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun navigateBack() {
        viewModelScope.launch {
            _eventFlow.emit(AddScheduleEvent.NavigateBack)
        }
    }
}