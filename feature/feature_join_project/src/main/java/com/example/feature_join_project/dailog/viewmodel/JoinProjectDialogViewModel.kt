package com.example.feature_join_project.dailog.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.feature_join_project.dailog.viewmodel.JoinProjectDialogEvent.*
import com.example.feature_join_project.dailog.viewmodel.JoinProjectDialogUiState.*
import dagger.hilt.android.lifecycle.HiltViewModel
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
    // 프로젝트 정보 데이터 클래스 (InviteValidationData 기반)
    data class ProjectInfo(
        val projectId: String,
        val projectName: String,
        val projectImage: String? = null,
        val inviterName: String? = null,
        val expiresAt: String? = null,
        val maxUses: Int? = null,
        val currentUses: Int? = null,
        val isAlreadyMember: Boolean = false
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
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinProjectDialogUiState())
    val uiState: StateFlow<JoinProjectDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinProjectDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    init {
        // Check if there's a pending invite code from navigation
        val pendingInviteCode = navigationManger.getResult<String>("dialog_invite_code")
        pendingInviteCode?.let { inviteCode ->
            setToken(inviteCode)
        }
    }

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

            try {
                // 실제 프로젝트 정보 로드 - ValidateInviteCodeUseCase 사용
                val projectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
                val result = projectUseCases.validateInviteCodeUseCase(token)

                when (result) {
                    is CustomResult.Success -> {
                        val inviteData = result.data
                        if (inviteData.valid) {
                            val projectInfo = ProjectInfo(
                                projectId = inviteData.projectId ?: "",
                                projectName = inviteData.projectName ?: "알 수 없는 프로젝트",
                                projectImage = inviteData.projectImage,
                                inviterName = null, // inviterName은 현재 InviteValidationData에 없음
                                expiresAt = inviteData.expiresAt?.toString(),
                                maxUses = inviteData.maxUses,
                                currentUses = inviteData.currentUses,
                                isAlreadyMember = inviteData.isAlreadyMember
                            )
                            _uiState.update { it.copy(isLoading = false, projectInfo = projectInfo) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = inviteData.errorMessage ?: "유효하지 않은 초대입니다."
                                )
                            }
                        }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error.message ?: "초대 정보를 불러오는데 실패했습니다."
                            )
                        }
                    }

                    is CustomResult.Initial -> TODO()
                    is CustomResult.Loading -> TODO()
                    is CustomResult.Progress -> TODO()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "초대 정보를 불러오는데 실패했습니다: ${e.message}"
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
        val projectInfo = _uiState.value.projectInfo
        if (token == null || _uiState.value.isJoining || _uiState.value.isLoading || projectInfo == null) {
            return // 토큰 없거나, 이미 참여 중이거나, 로딩 중이거나, 프로젝트 정보 없으면 무시
        }

        // 이미 멤버인 경우 처리
        if (projectInfo.isAlreadyMember) {
            viewModelScope.launch {
                _eventFlow.emit(JoinProjectDialogEvent.ShowSnackbar("이미 프로젝트에 참여하고 있습니다."))
                _eventFlow.emit(JoinProjectDialogEvent.JoinSuccess(projectInfo.projectId))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }
            println("ViewModel: Joining project with token: $token")

            try {
                // 실제 프로젝트 참여 로직 - JoinProjectWithCodeUseCase 사용
                val projectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
                val result = projectUseCases.joinProjectWithCodeUseCase(token)

                when (result) {
                    is CustomResult.Success -> {
                        val joinData = result.data
                        _eventFlow.emit(ShowSnackbar("프로젝트에 참여했습니다!"))
                        _eventFlow.emit(JoinSuccess(joinData))
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
                    it.copy(
                        isJoining = false,
                        error = "프로젝트 참여에 실패했습니다: ${e.message}"
                    )
                }
            }
        }
    }
}