package com.example.feature_join_project.dailog.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class JoinProjectDialogUiState(
    val token: String? = null,
    val isLoading: Boolean = true, // 초기 상태는 로딩
    val isJoining: Boolean = false, // 참여하기 요청 로딩 상태
    val projectInfo: ProjectInfo? = null, // 프로젝트 정보
    val error: String? = null
) {
    // 프로젝트 정보 데이터 클래스
    data class ProjectInfo(
        val projectName: String,
        val memberCount: Int // 예시 정보
        // TODO: 프로젝트 ID 등 필요한 정보 추가
    )
}

// --- 이벤트 ---
sealed class JoinProjectDialogEvent {
    object DismissDialog : JoinProjectDialogEvent()
    data class ShowSnackbar(val message: String) : JoinProjectDialogEvent()
    data class JoinSuccess(val projectId: String) : JoinProjectDialogEvent() // 참여 성공 알림
}

@HiltViewModel
class JoinProjectDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    // TODO: private val repository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinProjectDialogUiState())
    val uiState: StateFlow<JoinProjectDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinProjectDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 외부에서 토큰 설정 및 프로젝트 정보 로드 시작
     */
    fun setToken(token: String?) {
        if (token == null || token == _uiState.value.token) return // null이거나 이미 처리 중이면 무시
        _uiState.update { it.copy(token = token, isLoading = true, error = null, projectInfo = null) }
        loadProjectInfo(token)
    }

    /**
     * 토큰을 사용하여 프로젝트 정보 로드
     */
    private fun loadProjectInfo(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading project info for token: $token")

            // --- TODO: 실제 프로젝트 정보 로드 (repository.getProjectInfoFromToken) ---
            delay(800) // 임시 딜레이
            val success = !token.contains("invalid") // 임시 성공 조건
            // val result = repository.getProjectInfoFromToken(token)
            // ----------------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                // 임시 데이터
                val projectInfo = JoinProjectDialogUiState.ProjectInfo(
                    projectName = "초대된 프로젝트 (${token.takeLast(4)})",
                    memberCount = (token.length * 3) % 50 + 5 // 임의의 멤버 수
                )
                // val projectInfo = result.getOrThrow()
                _uiState.update { it.copy(isLoading = false, projectInfo = projectInfo) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "유효하지 않은 초대입니다." // result.exceptionOrNull()?.message ?: "정보 로드 실패"
                    )
                }
            }
        }
    }

    /**
     * '참여하기' 버튼 클릭 시 호출
     */
    fun joinProject() {
        val token = _uiState.value.token
        if (token == null || _uiState.value.isJoining || _uiState.value.isLoading || _uiState.value.projectInfo == null) {
            return // 토큰 없거나, 이미 참여 중이거나, 로딩 중이거나, 프로젝트 정보 없으면 무시
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }
            println("ViewModel: Joining project with token: $token")

            // --- TODO: 실제 프로젝트 참여 로직 (repository.joinProjectWithToken) ---
            delay(1000)
            val success = true
            val joinedProjectId = "joined_${token.takeLast(4)}" // 임시 ID
            // val result = repository.joinProjectWithToken(token)
            // -----------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                // val joinedProjectId = result.getOrThrow()
                _eventFlow.emit(JoinProjectDialogEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                _eventFlow.emit(JoinProjectDialogEvent.JoinSuccess(joinedProjectId)) // 성공 이벤트 (UI 닫기 및 후속 처리용)
                _uiState.update { it.copy(isJoining = false) } // 로딩 해제
            } else {
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        error = "프로젝트 참여에 실패했습니다." // result.exceptionOrNull()?.message ?: "참여 실패"
                    )
                }
            }
        }
    }
}