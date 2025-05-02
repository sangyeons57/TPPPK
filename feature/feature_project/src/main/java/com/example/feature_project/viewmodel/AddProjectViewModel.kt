package com.example.feature_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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


@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // 프로젝트 참여 로직
            val result = projectRepository.joinProjectWithCode(joinCode)
            
            if (result.isSuccess) {
                _eventFlow.emit(AddProjectEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                _uiState.update { it.copy(isLoading = false, projectAddedSuccessfully = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "참여 코드 확인 또는 참여 실패: ${result.exceptionOrNull()?.message ?: ""}"
                    ) 
                }
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
            
            // 프로젝트 생성 로직
            val isPublic = _uiState.value.createMode == CreateProjectMode.OPEN
            val result = projectRepository.createProject(name, description, isPublic)
            
            if (result.isSuccess) {
                _eventFlow.emit(AddProjectEvent.ShowSnackbar("프로젝트를 생성했습니다!"))
                _uiState.update { it.copy(isLoading = false, projectAddedSuccessfully = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "프로젝트 생성 실패: ${result.exceptionOrNull()?.message ?: ""}"
                    ) 
                }
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}