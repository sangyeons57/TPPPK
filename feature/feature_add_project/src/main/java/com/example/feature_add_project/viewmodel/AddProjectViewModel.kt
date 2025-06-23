package com.example.feature_add_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.project.CoreProjectUseCaseProvider
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
    val joinCode: String = "",
    val projectName: String = "",
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
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectUiState())
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProjectEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 사용자 인증 확인 및 UseCases 초기화
    private var currentUserId: String? = null
    
    init {
        // 사용자 인증 상태 확인
        initializeUserContext()
    }
    
    private fun initializeUserContext() {
        viewModelScope.launch {
            // 임시로 AuthRepository를 통해 사용자 정보 가져오기
            val tempUserUseCases = coreProjectUseCaseProvider.createForCurrentUser()
            when (val userSession = tempUserUseCases.authRepository.getCurrentUserSession()) {
                is CustomResult.Success -> {
                    currentUserId = userSession.data.userId
                }
                else -> {
                    _uiState.update { it.copy(errorMessage = "사용자 인증이 필요합니다.") }
                }
            }
        }
    }

    // 모드 변경 시
    fun onModeSelect(mode: AddProjectMode) {
        _uiState.update {
            it.copy(
                selectedMode = mode,
                // 모드 변경 시 입력값 초기화 (선택 사항)
                joinCode = "",
                projectName = "",
                errorMessage = null
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


    // "프로젝트 참여" 버튼 클릭
    fun onJoinProjectClick() {
        val joinCode = _uiState.value.joinCode
        if (joinCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "참여 코드를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 프로젝트 참여 로직 - Provider를 통한 UseCase 사용
            val userId = currentUserId
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 인증이 필요합니다.") }
                return@launch
            }
            
            // 프로젝트 참여를 위한 UseCases 생성
            val projectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
            val result = projectUseCases.joinProjectWithCodeUseCase(joinCode)

            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(AddProjectEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                    _uiState.update { it.copy(isLoading = false, projectAddedSuccessfully = true) }
                    
                    // 성공 시 이전 화면으로 돌아가기
                    navigationManger.navigateBack()
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "참여 코드 확인 또는 참여 실패: ${result.error.message ?: ""}"
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "참여 코드 확인 또는 참여 실패: 알수없는 에러"
                        )
                    }
                }
            }
        }
    }

    // "프로젝트 생성" 버튼 클릭
    fun onCreateProjectClick() {
        val name = _uiState.value.projectName
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "프로젝트 이름을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 프로젝트 생성 로직 - Provider를 통한 UseCase 사용
            val userId = currentUserId
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 인증이 필요합니다.") }
                return@launch
            }
            
            // 프로젝트 생성을 위한 UseCases 생성
            val projectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
            when (val result = projectUseCases.createProjectUseCase(name)) {
                is CustomResult.Success -> {
                    _eventFlow.emit(AddProjectEvent.ShowSnackbar("프로젝트를 생성했습니다!"))
                    _uiState.update { it.copy(isLoading = false, projectAddedSuccessfully = true) }
                    
                    // 성공 시 이전 화면으로 돌아가기
                    navigationManger.navigateBack()
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "프로젝트 생성 실패: ${result.error.message ?: ""}"
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "프로젝트 생성 실패: 알수없는 에러"
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