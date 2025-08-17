package com.example.feature_join_project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.core.MainContainerRoute
import com.example.core_navigation.core.NavigationManger
import com.example.domain_usecase.provider.project.CoreProjectUseCaseProvider
import com.example.domain_usecase.provider.project.CoreProjectUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private lateinit var coreProjectUseCases : CoreProjectUseCases

    private val _uiState = MutableStateFlow(JoinProjectUiState())
    val uiState: StateFlow<JoinProjectUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinProjectEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    init {
        viewModelScope.launch {
            coreProjectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
        }
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
        // TODO: 이 화면은 더 이상 사용하지 않습니다. (프로젝트ID 기반 다이얼로그 사용)
        viewModelScope.launch {
            _eventFlow.emit(JoinProjectEvent.ShowSnackbar("TODO: 이 화면은 비활성화되었습니다."))
        }
    }
}
