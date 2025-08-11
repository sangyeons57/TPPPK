package com.example.feature_member_list.dialog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ProjectId
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.auth.AuthSessionUseCases
import com.example.domain_usecase.provider.friend.FriendUseCaseProvider
import com.example.domain_usecase.provider.friend.FriendUseCases
import com.example.domain_usecase.provider.dm.DMUseCaseProvider
import com.example.domain_usecase.provider.dm.DMUseCases
import com.example.domain_usecase.usecase.user.SearchUsersByNameUseCaseImpl
import com.example.domain_usecase.usecase.project.SendMemberInvitationDMUseCase
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectMemberUseCases
import com.example.feature_member_list.dialog.ui.FriendItem
import com.example.feature_member_list.dialog.ui.SearchedUser
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
    val friends: List<FriendItem> = emptyList(), // 친구 목록
    val selectedMembers: Set<UserId> = emptySet(), // 선택된 멤버들 (친구 + 검색된 사용자)
    val searchQuery: String = "", // 사용자 이름 검색 쿼리
    val searchedUsers: List<SearchedUser> = emptyList(), // 검색된 사용자 목록
    val isLoadingFriends: Boolean = false, // 친구 목록 로딩
    val isLoadingSearch: Boolean = false, // 사용자 검색 로딩
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
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val friendUseCaseProvider: FriendUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val searchUsersByNameUseCase: SearchUsersByNameUseCaseImpl,
    private val sendMemberInvitationDMUseCase: SendMemberInvitationDMUseCase
) : ViewModel() {

    private var projectMemberUseCases: ProjectMemberUseCases? = null
    private var dmUseCases: DMUseCases? = null
    private var friendUseCases: FriendUseCases? = null
    private var authSessionUseCases: AuthSessionUseCases? = null

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
    suspend fun loadFriends() {
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
     * 사용자 이름으로 검색합니다.
     */
    fun searchUserByName(userName: String) {
        if (userName.isBlank()) {
            _uiState.update { it.copy(searchedUsers = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSearch = true, error = null) }
            
            try {
                searchUsersByNameUseCase(userName, 10).collect { result ->
                    when (result) {
                        is CustomResult.Success -> {
                            val users = result.data
                            val searchedUsers = users.map { user ->
                                SearchedUser(
                                    userId = UserId(user.id.value),
                                    userName = user.name,
                                    userEmail = user.email.value,
                                    profileImageUrl = null // User model doesn't have profileImageUrl property
                                )
                            }
                            _uiState.update {
                                it.copy(
                                    searchedUsers = searchedUsers,
                                    isLoadingSearch = false,
                                    error = null
                                )
                            }
                        }

                        is CustomResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    searchedUsers = emptyList(),
                                    isLoadingSearch = false,
                                    error = "사용자 검색 실패: ${result.error.message}"
                                )
                            }
                        }

                        is CustomResult.Loading -> {
                            // 로딩 상태는 이미 설정됨
                        }

                        else -> {
                            // 기타 상태 처리
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchedUsers = emptyList(),
                        isLoadingSearch = false,
                        error = "사용자 검색 실패: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 검색 쿼리 변경을 처리합니다.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // 검색어가 비어있으면 검색 결과 정리
        if (query.isBlank()) {
            _uiState.update { it.copy(searchedUsers = emptyList()) }
        }
    }

    /**
     * 멤버 선택 상태를 변경합니다.
     */
    fun onMemberSelectionChanged(userId: UserId, isSelected: Boolean) {
        _uiState.update { currentState ->
            val newSelectedMembers = currentState.selectedMembers.toMutableSet()
            if (isSelected) {
                newSelectedMembers.add(userId)
            } else {
                newSelectedMembers.remove(userId)
            }
            currentState.copy(selectedMembers = newSelectedMembers)
        }
    }

    /**
     * 선택된 멤버들에게 DM으로 프로젝트 초대를 보냅니다.
     */
    fun inviteMembers(projectId: DocumentId, selectedMemberIds: Set<UserId>) {
        if (selectedMemberIds.isEmpty()) {
            viewModelScope.launch {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("초대할 사용자를 선택해주세요.")) 
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true) }
            var successCount = 0
            var failureCount = 0

            try {
                for (memberId in selectedMemberIds) {
                    try {
                        // 대상 사용자 이름 찾기 (친구 목록에서 또는 검색 결과에서)
                        val targetUserName = findUserNameById(memberId)
                        if (targetUserName != null) {
                            // SendMemberInvitationDMUseCase 사용하여 DM 초대 메시지 전송
                            sendMemberInvitationDMUseCase(
                                targetUserId = memberId,
                                targetUserName = targetUserName,
                                projectId = projectId
                            )
                                .collect { result ->
                                    when (result) {
                                        is CustomResult.Success -> {
                                            successCount++
                                        }

                                        is CustomResult.Failure -> {
                                            failureCount++
                                        }

                                        is CustomResult.Loading -> {
                                            // 로딩 상태
                                        }

                                        else -> {
                                            // 기타 상태
                                        }
                                    }
                                }
                        } else {
                            failureCount++
                        }
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingFriends = false,
                        error = "초대 중 오류 발생: ${e.message}"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoadingFriends = false,
                    addSuccess = failureCount == 0,
                    selectedMembers = emptySet() // 선택 초기화
                )
            }

            if (successCount > 0) {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${successCount}명에게 DM 초대를 보냈습니다."))
                _eventFlow.emit(AddMemberDialogEvent.MembersAddedSuccessfully)
            }
            if (failureCount > 0) {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${failureCount}명의 초대에 실패했습니다."))
            }
        }
    }

    /**
     * 사용자 ID로 사용자 이름을 찾습니다.
     */
    private fun findUserNameById(userId: UserId): UserName? {
        val currentState = _uiState.value

        // 친구 목록에서 찾기
        val friend = currentState.friends.find { it.userId == userId }
        if (friend != null) {
            return friend.userName
        }

        // 검색 결과에서 찾기
        val searchedUser = currentState.searchedUsers.find { it.userId == userId }
        if (searchedUser != null) {
            return searchedUser.userName
        }

        return null
    }

}
