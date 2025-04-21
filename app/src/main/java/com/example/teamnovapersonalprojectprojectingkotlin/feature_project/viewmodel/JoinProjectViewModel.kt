package com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class JoinProjectUiState(
    val inviteCodeOrLink: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
    // joinSuccess는 Event로 처리
)

// --- 이벤트 ---
sealed class JoinProjectEvent {
    data class JoinSuccess(val projectId: String) : JoinProjectEvent() // 성공 시 참여한 프로젝트 ID 전달 가능
    data class ShowSnackbar(val message: String) : JoinProjectEvent()
    object ClearFocus : JoinProjectEvent()
}


@HiltViewModel
class JoinProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    // TODO: private val repository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinProjectUiState())
    val uiState: StateFlow<JoinProjectUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinProjectEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 초대 코드 또는 링크 입력 변경 시 호출
     */
    fun onCodeOrLinkChange(input: String) {
        _uiState.update {
            it.copy(inviteCodeOrLink = input, error = null) // 에러 초기화
        }
    }

    /**
     * '프로젝트 참여하기' 버튼 클릭 시 호출
     */
    fun joinProject() {
        val codeOrLink = _uiState.value.inviteCodeOrLink.trim()

        if (codeOrLink.isBlank()) {
            _uiState.update { it.copy(error = "초대 링크 또는 코드를 입력해주세요.") }
            return
        }

        // 간단한 URL 또는 코드 형식 검사 (선택적)
        // val isLikelyUrl = codeOrLink.startsWith("http://") || codeOrLink.startsWith("https://")
        // val isValidFormat = isLikelyUrl || codeOrLink.length > 5 // 예시: 코드는 5자리 이상이라고 가정

        if (_uiState.value.isLoading) return // 로딩 중 중복 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(JoinProjectEvent.ClearFocus) // 키보드 숨기기 요청
            println("ViewModel: Attempting to join project with code/link: $codeOrLink")

            // --- TODO: 실제 프로젝트 참여 로직 (repository.joinProjectWithCode) ---
            delay(1200) // 임시 딜레이
            val success = codeOrLink != "invalid-code" // 임시 성공 조건
            val joinedProjectId = if(success) "joined_project_123" else ""
            // val result = repository.joinProjectWithCode(codeOrLink)
            // -----------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                // val joinedProjectId = result.getOrThrow() // 성공 시 ID 가져오기
                _eventFlow.emit(JoinProjectEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                _eventFlow.emit(JoinProjectEvent.JoinSuccess(joinedProjectId)) // 성공 이벤트 발생
                _uiState.update { it.copy(isLoading = false) } // 로딩 해제 (네비게이션은 Screen에서 처리)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "유효하지 않은 초대 코드 또는 링크입니다." // result.exceptionOrNull()?.message ?: "참여 실패"
                    )
                }
            }
        }
    }
}