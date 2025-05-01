package com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.ProjectItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// 프로젝트 추가 모드
enum class AddProjectMode {
    JOIN, CREATE
}

enum class CreateProjectMode {
    OPEN, CLOSE
}

// 프로젝트 추가 UI 상태
data class AddProjectUiState(
    val selectedMode: AddProjectMode = AddProjectMode.JOIN, // 기본: 참여
    val createMode: CreateProjectMode = CreateProjectMode.OPEN, // 기본: 참여
    val joinCode: String = "",
    val projectName: String = "",
    val projectDescription: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val projectAddedSuccessfully: Boolean = false // 성공 시 이전 화면 이동 트리거
)

// 프로젝트 추가 이벤트
sealed class AddProjectEvent {
    data class ShowSnackbar(val message: String) : AddProjectEvent()
    // 성공 이벤트는 UiState 플래그로 처리
}

// --- Repository 인터페이스 정의 (기존 ProjectRepository 확장 또는 신규) ---
interface ProjectRepository {
    suspend fun getProjectList(): Result<List<ProjectItem>> // Home에서 사용
    suspend fun joinProject(joinCode: String): Result<Unit> // 프로젝트 참여
    suspend fun createProject(name: String, description: String): Result<Unit> // 프로젝트 생성
}
// -------------------------------------------------------------------

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    // TODO: private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectUiState())
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProjectEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 모드 변경 시
    fun onModeSelect(mode: AddProjectMode) {
        _uiState.update {
            it.copy(
                selectedMode = mode,
                // 모드 변경 시 입력값 초기화 (선택 사항)
                joinCode = "",
                projectName = "",
                projectDescription = "",
                errorMessage = null
            )
        }
    }

    fun onCreateModeSelect(mode: CreateProjectMode) {
        _uiState.update {
            it.copy(
                createMode = mode,
                // 모드 변경 시 입력값 초기화 (선택 사항)
            )
        }
    }

    // 입력값 변경 처리
    fun onJoinCodeChange(code: String) {
        _uiState.update { it.copy(joinCode = code, errorMessage = null) }
    }

    fun onProjectNameChange(name: String) {
        _uiState.update { it.copy(projectName = name, errorMessage = null) }
    }

    fun onProjectDescriptionChange(description: String) {
        _uiState.update { it.copy(projectDescription = description, errorMessage = null) }
    }

    // "프로젝트 참여" 버튼 클릭
    fun onJoinProjectClick() {
        val joinCode = _uiState.value.joinCode
        if (joinCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "참여 코드를 입력해주세요.") }
            return
        }
        // TODO: 참여 코드 유효성 검사 추가 가능

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 프로젝트 참여 시도 - 코드: $joinCode")
            // --- TODO: 실제 프로젝트 참여 로직 (projectRepository.joinProject(joinCode)) ---
            kotlinx.coroutines.delay(1000) // 임시 딜레이
            val success = true // 임시 성공
            // val result = projectRepository.joinProject(joinCode)
            // ---------------------------------------------------------------------
            if (success) {
                _eventFlow.emit(AddProjectEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                _uiState.update { it.copy(isLoading = false, projectAddedSuccessfully = true) } // 성공 플래그 설정
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "참여 코드 확인 또는 참여 실패") }
            }
        }
    }

    // "프로젝트 생성" 버튼 클릭
    fun onCreateProjectClick() {
        val name = _uiState.value.projectName
        val description = _uiState.value.projectDescription
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "프로젝트 이름을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 프로젝트 생성 시도 - 이름: $name")
            // --- TODO: 실제 프로젝트 생성 로직 (projectRepository.createProject(name, description)) ---
            kotlinx.coroutines.delay(1000) // 임시 딜레이
            val success = true // 임시 성공
            // val result = projectRepository.createProject(name, description)
            // ------------------------------------------------------------------------
            if (success) {
                _eventFlow.emit(AddProjectEvent.ShowSnackbar("프로젝트를 생성했습니다!"))
                _uiState.update { it.copy(isLoading = false, projectAddedSuccessfully = true) } // 성공 플래그 설정
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "프로젝트 생성 실패") }
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}