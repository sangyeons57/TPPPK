package com.example.feature_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.routes.AppRoutes // 업데이트된 AppRoutes 경로로 수정
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 프로젝트 상세 화면 UI 상태
data class ProjectDetailUiState(
    val projectId: String? = null,
    val projectName: String = "프로젝트 이름 로딩 중...",
    val isLoading: Boolean = true,
    val error: String? = null
)

// 프로젝트 상세 화면 이벤트
sealed class ProjectDetailEvent {
    // 필요한 경우 이벤트 추가 (예: 설정 화면으로 이동)
    data class NavigateToProjectSettings(val projectId: String) : ProjectDetailEvent()
}

/**
 * 프로젝트 상세 정보를 관리하는 ViewModel.
 * @param savedStateHandle Navigation argument를 받기 위한 SavedStateHandle.
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
    // 필요한 UseCase 주입 (예: GetProjectDetailsUseCase)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    // 이벤트 처리를 위한 SharedFlow (필요한 경우)
    // private val _eventFlow = MutableSharedFlow<ProjectDetailEvent>()
    // val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            val projectId = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) // AppRoutes에서 ARG_PROJECT_ID 가져오기
            if (projectId == null) {
                _uiState.update { it.copy(isLoading = false, error = "프로젝트 ID를 찾을 수 없습니다.") }
                return@launch
            }
            _uiState.update { it.copy(projectId = projectId, isLoading = true) }
            // TODO: 실제 프로젝트 상세 정보 로드 로직 구현 (UseCase 사용)
            // 예시: val projectDetails = getProjectDetailsUseCase(projectId)
            // 성공 시: _uiState.update { it.copy(projectName = projectDetails.name, isLoading = false) }
            // 실패 시: _uiState.update { it.copy(error = "정보 로드 실패", isLoading = false) }

            // 임시 데이터 (실제 구현 시 삭제)
            kotlinx.coroutines.delay(1000) // 가상 로딩 시간
            _uiState.update {
                it.copy(
                    projectName = "프로젝트 '$projectId' 상세 (임시)",
                    isLoading = false
                )
            }
        }
    }

    // 필요한 경우 이벤트 핸들러 함수 추가
    // fun onProjectSettingsClicked() {
    //     val projectId = uiState.value.projectId
    //     if (projectId != null) {
    //         viewModelScope.launch {
    //             _eventFlow.emit(ProjectDetailEvent.NavigateToProjectSettings(projectId))
    //         }
    //     }
    // }
} 