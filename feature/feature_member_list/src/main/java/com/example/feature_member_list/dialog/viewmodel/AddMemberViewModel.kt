package com.example.feature_member_list.dialog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.provider.project.ProjectMemberUseCaseProvider
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.friend.FriendUseCaseProvider
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.feature_member_list.dialog.ui.FriendItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMemberDialogUiState(
    val projectInviteLink: String? = null, // 프로젝트 참가 링크
    val friends: List<FriendItem> = emptyList(), // 친구 목록
    val selectedFriends: Set<UserId> = emptySet(), // 선택된 친구들
    val isLoadingLink: Boolean = false, // 링크 생성 로딩
    val isLoadingFriends: Boolean = false, // 친구 목록 로딩
    val error: String? = null,
    val addSuccess: Boolean = false
)

sealed class AddMemberDialogEvent {
    data class ShowSnackbar(val message: String) : AddMemberDialogEvent()
    object DismissDialog : AddMemberDialogEvent()
    object MembersAddedSuccessfully : AddMemberDialogEvent()
}

@HiltViewModel
class AddMemberViewModel @Inject constructor(
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val friendUseCaseProvider: FriendUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider
) : ViewModel() {

    private var projectMemberUseCases: com.example.domain.provider.project.ProjectMemberUseCases? = null
    private var coreProjectUseCases: com.example.domain.provider.project.CoreProjectUseCases? = null
    private var friendUseCases: com.example.domain.provider.friend.FriendUseCases? = null
    private var authSessionUseCases: com.example.domain.provider.auth.AuthSessionUseCases? = null

    private val _uiState = MutableStateFlow(AddMemberDialogUiState())
    val uiState: StateFlow<AddMemberDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddMemberDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        authSessionUseCases = authSessionUseCaseProvider.create()
    }

    /**
     * 친구 목록을 로드합니다.
     */
    fun loadFriends() {
        if (friendUseCases == null) {
            // 현재 사용자 ID를 가져와서 FriendUseCases 생성
            friendUseCases = friendUseCaseProvider.createForCurrentUser() // 임시로 사용
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true, error = null) }
            
            try {
                val useCases = friendUseCases ?: return@launch
                useCases.getFriendsListStreamUseCase().collect { friendsResult ->
                    when (friendsResult) {
                        is CustomResult.Success -> {
                            val friends = friendsResult.data
                            val friendItems = friends.map { friend ->
                                FriendItem(
                                    userId = UserId(friend.id.value), // DocumentId를 UserId로 변환
                                    userName = friend.name, // Friend 모델의 Name 타입
                                    userEmail = null, // 이메일은 추가 조회 필요 (나중에 개선)
                                    profileImageUrl = friend.profileImageUrl?.value,
                                    isOnline = false // 온라인 상태는 나중에 추가
                                )
                            }
                            _uiState.update { 
                                it.copy(
                                    friends = friendItems, 
                                    isLoadingFriends = false, 
                                    error = null
                                ) 
                            }
                        }
                        is CustomResult.Failure -> {
                            _uiState.update { 
                                it.copy(
                                    isLoadingFriends = false, 
                                    error = "친구 목록 로딩 실패: ${friendsResult.error.message}"
                                ) 
                            }
                        }
                        else -> {
                            // Loading, Initial, Progress 상태 처리
                            _uiState.update { it.copy(isLoadingFriends = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingFriends = false, 
                        error = "친구 목록 로딩 실패: ${e.message}"
                    ) 
                }
            }
        }
    }

    /**
     * 프로젝트 초대 링크를 로드합니다.
     */
    fun loadProjectInviteLink(projectId: DocumentId) {
        // 기존 초대 링크가 있다면 생성하지 않고 바로 새로운 링크 생성을 권장
        generateProjectInviteLink(projectId)
    }

    /**
     * 새로운 프로젝트 초대 링크를 생성합니다.
     */
    fun generateProjectInviteLink(projectId: DocumentId) {
        // CoreProjectUseCases 초기화
        if (coreProjectUseCases == null && authSessionUseCases != null) {
            val currentUser = authSessionUseCases!!.authRepository.getCurrentUserSession()
            when (currentUser) {
                is CustomResult.Success -> {
                    coreProjectUseCases = coreProjectUseCaseProvider.createForProject(
                        projectId = projectId,
                        userId = currentUser.data.userId
                    )
                }
                else -> {
                    viewModelScope.launch {
                        _uiState.update { 
                            it.copy(
                                isLoadingLink = false, 
                                error = "사용자 인증 정보를 가져올 수 없습니다."
                            ) 
                        }
                    }
                    return
                }
            }
        }

        val useCases = coreProjectUseCases ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLink = true, error = null) }
            
            try {
                val currentUser = authSessionUseCases!!.authRepository.getCurrentUserSession()
                when (currentUser) {
                    is CustomResult.Success -> {
                        // 1. 초대 생성 (DocumentId 반환)
                        val createResult = useCases.generateInviteLinkUseCase(
                            inviterId = currentUser.data.userId,
                            projectId = projectId,
                            expiresInHours = 24 // 기본 24시간
                        )
                        
                        when (createResult) {
                            is CustomResult.Success -> {
                                // 2. 생성된 DocumentId로 링크 생성
                                val invitationId = createResult.data
                                val inviteLink = useCases.generateInviteLinkFromIdUseCase(invitationId)
                                
                                _uiState.update { 
                                    it.copy(
                                        projectInviteLink = inviteLink,
                                        isLoadingLink = false,
                                        error = null
                                    ) 
                                }
                                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("새로운 초대 링크가 생성되었습니다."))
                            }
                            is CustomResult.Failure -> {
                                _uiState.update { 
                                    it.copy(
                                        isLoadingLink = false, 
                                        error = "초대 링크 생성 실패: ${createResult.error.message}"
                                    ) 
                                }
                            }
                            else -> {
                                _uiState.update { it.copy(isLoadingLink = true) }
                            }
                        }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isLoadingLink = false, 
                                error = "사용자 인증 실패: ${currentUser.error.message}"
                            ) 
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoadingLink = true) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingLink = false, 
                        error = "초대 링크 생성 중 오류 발생: ${e.message}"
                    ) 
                }
            }
        }
    }

    /**
     * 친구 선택 상태를 변경합니다.
     */
    fun onFriendSelectionChanged(userId: UserId, isSelected: Boolean) {
        _uiState.update { currentState ->
            val newSelectedFriends = currentState.selectedFriends.toMutableSet()
            if (isSelected) {
                newSelectedFriends.add(userId)
            } else {
                newSelectedFriends.remove(userId)
            }
            currentState.copy(selectedFriends = newSelectedFriends)
        }
    }

    /**
     * 선택된 친구들에게 프로젝트 초대를 보냅니다.
     */
    fun inviteFriends(projectId: DocumentId, selectedFriendIds: Set<UserId>) {
        if (selectedFriendIds.isEmpty()) {
            viewModelScope.launch { 
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("초대할 친구를 선택해주세요.")) 
            }
            return
        }

        // 프로젝트별 UseCase 그룹 생성
        if (projectMemberUseCases == null) {
            projectMemberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
        }
        val useCases = projectMemberUseCases ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true) }
            var allSuccess = true
            var successCount = 0

            // TODO: 실제로는 친구에게 프로젝트 참가 요청을 보내는 기능 구현
            // 현재는 프로젝트 멤버로 직접 추가하는 방식으로 임시 구현
            for (friendId in selectedFriendIds) {
                try {
                    val result = useCases.addProjectMemberUseCase(
                        userId = friendId,
                        initialRoleIds = emptyList() // 기본 역할 없음
                    )
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        allSuccess = false
                        val friendName = _uiState.value.friends
                            .find { it.userId == friendId }?.userName?.value ?: "알 수 없는 사용자"
                        _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${friendName}님 초대 실패"))
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("초대 중 오류 발생: ${e.message}"))
                }
            }

            _uiState.update {
                it.copy(
                    isLoadingFriends = false,
                    addSuccess = allSuccess,
                    selectedFriends = emptySet() // 선택 초기화
                )
            }

            if (successCount > 0) {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${successCount}명의 친구에게 초대를 보냈습니다."))
                _eventFlow.emit(AddMemberDialogEvent.MembersAddedSuccessfully)
            }
            if (!allSuccess) {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("일부 친구 초대에 실패했습니다."))
            }
        }
    }

    /**
     * 초대 링크 복사 완료 처리
     */
    fun onInviteLinkCopied(link: String) {
        viewModelScope.launch {
            _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("초대 링크가 클립보드에 복사되었습니다."))
        }
    }
}
