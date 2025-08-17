package com.example.feature_join_project.dailog.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain_usecase.provider.project.CoreProjectUseCaseProvider
import com.example.feature_join_project.dailog.viewmodel.JoinProjectDialogEvent.JoinSuccess
import com.example.feature_join_project.dailog.viewmodel.JoinProjectDialogEvent.ShowSnackbar
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
data class JoinProjectDialogUiState(
    val projectId: String? = null,
    val projectName: String? = null,
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class JoinProjectDialogEvent {
    object DismissDialog : JoinProjectDialogEvent()
    data class ShowSnackbar(val message: String) : JoinProjectDialogEvent()
    data class JoinSuccess(val projectId: String) : JoinProjectDialogEvent() // 참여 성공 알림
}

@HiltViewModel
class JoinProjectDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinProjectDialogUiState())
    val uiState: StateFlow<JoinProjectDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinProjectDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    init {
        Log.d("JoinProjectDialogVM", "[INFO] 현재 이 다이얼로그는 기본 흐름에서 사용되지 않습니다 (chat에서 바로 참여)")
        // Navigation에서 전달된 projectId가 있으면 설정 (호환: 기존 invite_code 키도 확인)
        val pendingProjectId = navigationManger.getResult<String>("dialog_project_id")
            ?: navigationManger.getResult("dialog_invite_code")
        pendingProjectId?.let { setProjectId(it) }
    }

    fun setProjectId(projectId: String?) {
        if (projectId == null || projectId == _uiState.value.projectId) return
        _uiState.update { it.copy(projectId = projectId, error = null) }
    }

    /**
     * '참여하기' 버튼 클릭 시 호출
     */
    fun joinProject() {
        val projectId = _uiState.value.projectId
        if (projectId == null || _uiState.value.isJoining) return

        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }
            println("ViewModel: Joining project with projectId: $projectId")

            try {
                // 프로젝트 참여 로직 - JoinProjectByIdUseCase 사용
                val projectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
                val result = projectUseCases.joinProjectByIdUseCase(projectId)

                when (result) {
                    is CustomResult.Success -> {
                        val joinedProjectId = result.data
                        _eventFlow.emit(ShowSnackbar("프로젝트에 참여했습니다!"))
                        _eventFlow.emit(JoinSuccess(joinedProjectId))
                        _uiState.update { it.copy(isJoining = false) }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isJoining = false,
                                error = result.error.message ?: "프로젝트 참여에 실패했습니다."
                            )
                        }
                    }

                    is CustomResult.Initial -> TODO()
                    is CustomResult.Loading -> TODO()
                    is CustomResult.Progress -> TODO()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isJoining = false, error = "프로젝트 참여에 실패했습니다: ${e.message}")
                }
            }
        }
    }
}
