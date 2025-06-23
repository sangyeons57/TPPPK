package com.example.feature_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class SetProjectNameUiState(
    val projectName: String = "",
    val isLoading: Boolean = false, // 이름 중복 확인 등 비동기 작업 시 사용 가능
    val error: String? = null
)

// --- 네비게이션 이벤트 정의 ---
sealed class SetProjectNameNavigationEvent {
    object NavigateToNextStep : SetProjectNameNavigationEvent() // 다음 생성 단계로 이동
    object NavigateBack : SetProjectNameNavigationEvent()
}

// --- 화면 내 이벤트 정의 ---
sealed class SetProjectNameEvent {
    data class ShowSnackbar(val message: String) : SetProjectNameEvent()
    data class Navigate(val destination: SetProjectNameNavigationEvent) : SetProjectNameEvent()
    object ClearFocus : SetProjectNameEvent()
}


@HiltViewModel
class SetProjectNameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle, // 이전 단계 정보나 결과 저장/복원 위해 유지
    // TODO: private val repository: ProjectRepository // 이름 중복 확인 등 필요 시 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetProjectNameUiState())
    val uiState: StateFlow<SetProjectNameUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SetProjectNameEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // 이전 단계에서 전달받은 임시 프로젝트 이름이 있다면 복원 (선택적)
        val initialName = savedStateHandle.get<String>("tempProjectName") ?: ""
        _uiState.update { it.copy(projectName = initialName) }
    }

    /**
     * 프로젝트 이름 입력 변경 시 호출
     */
    fun onProjectNameChange(name: String) {
        _uiState.update {
            it.copy(projectName = name, error = null) // 에러 초기화
        }
        // 이름 변경 시 임시 저장 (선택적)
        // savedStateHandle["tempProjectName"] = name
    }

    /**
     * '다음' 또는 '완료' 버튼 클릭 시 호출
     */
    fun onNextClick() {
        val currentName = _uiState.value.projectName.trim()

        // 이름 유효성 검사
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "프로젝트 이름을 입력해주세요.") }
            return
        }
        // TODO: 추가 유효성 검사 (길이, 특수문자 등)

        // TODO: 프로젝트 이름 중복 확인 등 비동기 검증 필요 시 로딩 상태 활성화 및 Repository 호출
        // viewModelScope.launch {
        //     _uiState.update { it.copy(isLoading = true, error = null) }
        //     val result = repository.isProjectNameAvailable(currentName)
        //     if (result.isSuccess && result.getOrThrow()) {
        //         // 사용 가능한 이름 -> 다음 단계로 이동
        //         savedStateHandle["finalProjectName"] = currentName // 최종 이름 저장
        //         _eventFlow.emit(SetProjectNameEvent.Navigate(SetProjectNameNavigationEvent.NavigateToNextStep))
        //     } else {
        //         // 사용 불가능한 이름 또는 오류
        //         _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "사용할 수 없는 이름입니다.") }
        //     }
        // }

        // --- 임시 구현: 유효성 검사 통과 시 바로 다음 단계로 이동 ---
        viewModelScope.launch {
            _eventFlow.emit(SetProjectNameEvent.ClearFocus)
            // TODO: 다음 단계로 프로젝트 이름 전달 (예: SavedStateHandle 또는 네비게이션 인자)
            // savedStateHandle["finalProjectName"] = currentName // 다른 ViewModel에서 사용하도록 저장
            println("ViewModel: Project name set to '$currentName', navigating to next step.")
            _eventFlow.emit(SetProjectNameEvent.Navigate(SetProjectNameNavigationEvent.NavigateToNextStep))
        }
        // -------------------------------------------------------
    }
}