package com.example.feature_join_project.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.MainContainerRoute
import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val navigationManger: NavigationManger,
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val coreProjectUseCases = coreProjectUseCaseProvider.createForCurrentUser()

    private val _uiState = MutableStateFlow(JoinProjectUiState())
    val uiState: StateFlow<JoinProjectUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinProjectEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    init {
        // 딥링크로부터 전달된 초대 코드가 있는지 확인
        val pendingInviteCode = navigationManger.getResult<String>("pending_invite_code")
        pendingInviteCode?.let { inviteCode ->
            onCodeOrLinkChange(inviteCode)
            // 초대 코드를 자동으로 설정했음을 사용자에게 알림 (선택적)
            viewModelScope.launch {
                _eventFlow.emit(JoinProjectEvent.ShowSnackbar("초대 링크가 자동으로 입력되었습니다."))
            }
        }
    }

    /**
     * 초대 코드 또는 링크 입력 변경 시 호출
     */
    fun onCodeOrLinkChange(input: String) {
        _uiState.update {
            it.copy(inviteCodeOrLink = input, error = null) // 에러 초기화
        }
    }

    fun navigateToClearingBackStack() {
        navigationManger.navigateToClearingBackStack(MainContainerRoute)
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

            // 프로젝트 참여 로직
            val result = coreProjectUseCases.joinProjectWithCodeUseCase(codeOrLink)

            when (result){
                is CustomResult.Success -> {
                    val joinedProjectId = result.data // 성공 시 ID 가져오기
                    _eventFlow.emit(JoinProjectEvent.ShowSnackbar("프로젝트에 참여했습니다!"))
                    _eventFlow.emit(JoinProjectEvent.JoinSuccess(joinedProjectId)) // 성공 이벤트 발생
                    _uiState.update { it.copy(isLoading = false) } // 로딩 해제 (네비게이션은 Screen에서 처리)
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "유효하지 않은 초대 코드 또는 링크입니다: ${result.error}"
                        )
                    }
                }
                else  ->{
                    _uiState.update { it.copy(isLoading = false) }
                    Log.e("JoinProjectViewModel", "Unknown result type: $result")
                }
            }
        }
    }
}