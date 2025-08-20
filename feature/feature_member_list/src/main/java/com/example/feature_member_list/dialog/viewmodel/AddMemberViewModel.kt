package com.example.feature_member_list.dialog.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.vo.ChannelId
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
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCases
import com.example.domain_usecase.provider.project.ProjectMemberUseCases
import com.example.domain_usecase.provider.project.CoreProjectUseCaseProvider
import kotlinx.coroutines.flow.first
import com.example.feature_member_list.dialog.ui.FriendItem
import com.example.feature_member_list.dialog.ui.SearchedUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
) : ViewModel() {

    private var projectMemberUseCases: ProjectMemberUseCases? = null
    private var dmUseCases: DMUseCases? = null
    private var friendUseCases: FriendUseCases? = null
    private var authSessionUseCases: AuthSessionUseCases? = null
    private var userUseCases: UserUseCases? = null

    private val _uiState = MutableStateFlow(AddMemberDialogUiState())
    val uiState: StateFlow<AddMemberDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddMemberDialogEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        authSessionUseCases = authSessionUseCaseProvider.create()
        userUseCases = userUseCaseProvider.createForUser()
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
                                    userId = UserId.from(friend.id), // DocumentId를 UserId로 변환
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
                                    userId = UserId.from(user.id),
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
        Log.d(
            "AddMemberViewModel",
            "inviteMembers 시작 - projectId: ${projectId.value}, memberCount: ${selectedMemberIds.size}"
        )
        
        if (selectedMemberIds.isEmpty()) {
            Log.w("AddMemberViewModel", "선택된 멤버가 없음")
            viewModelScope.launch {
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("초대할 사용자를 선택해주세요.")) 
            }
            return
        }

        Log.d("AddMemberViewModel", "초대 대상 멤버들: ${selectedMemberIds.map { it.value }}")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true) }
            var successCount = 0
            var failureCount = 0

            try {
                // 현재 사용자 세션과 사용자 ID/이름 확보
                Log.d("AddMemberViewModel", "현재 사용자 세션 가져오는 중...")
                val sessionResult = authSessionUseCases?.getCurrentUserSessionUseCase?.invoke()
                val currentUserId = when (sessionResult) {
                    is CustomResult.Success -> {
                        Log.d(
                            "AddMemberViewModel",
                            "현재 사용자 세션 확인됨: ${sessionResult.data.userId.value}"
                        )
                        sessionResult.data.userId
                    }
                    is CustomResult.Failure -> {
                        Log.e("AddMemberViewModel", "세션 정보 가져오기 실패", sessionResult.error)
                        _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("세션 정보를 가져오지 못했습니다."))
                        _uiState.update { it.copy(isLoadingFriends = false) }
                        return@launch
                    }

                    else -> {
                        Log.e("AddMemberViewModel", "사용자 세션 상태가 올바르지 않음: $sessionResult")
                        _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("로그인이 필요합니다."))
                        _uiState.update { it.copy(isLoadingFriends = false) }
                        return@launch
                    }
                }

                // 프로젝트 이름 확보
                Log.d("AddMemberViewModel", "프로젝트 정보 가져오는 중: ${projectId.value}")
                val coreProjectUseCases =
                    coreProjectUseCaseProvider.createForProject(projectId, currentUserId)
                val projectResult =
                    coreProjectUseCases.getProjectDetailsStreamUseCase(projectId).firstOrNull()
                val projectName = when (projectResult) {
                    is CustomResult.Success -> {
                        Log.d("AddMemberViewModel", "프로젝트 정보 확인됨: ${projectResult.data.name.value}")
                        projectResult.data.name.value
                    }

                    else -> {
                        Log.w("AddMemberViewModel", "프로젝트 정보를 가져올 수 없음, 기본값 사용")
                        "프로젝트"
                    }
                }

                // DM UseCases 생성 (현재 사용자 기준)
                Log.d("AddMemberViewModel", "DM UseCases 생성 중...")
                if (dmUseCases == null) {
                    dmUseCases = dmUseCaseProvider.createForUser(currentUserId)
                }

                // ProjectMember UseCases 생성
                Log.d("AddMemberViewModel", "ProjectMember UseCases 생성 중...")
                if (projectMemberUseCases == null) {
                    projectMemberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
                    Log.d("AddMemberViewModel", "ProjectMember UseCases 생성 완료")
                } else {
                    Log.d("AddMemberViewModel", "ProjectMember UseCases 이미 존재함")
                }

                Log.d("AddMemberViewModel", "멤버별 초대 메시지 전송 시작")
                for (memberId in selectedMemberIds) {
                    Log.d("AddMemberViewModel", "멤버 초대 처리 중: ${memberId.value}")
                    try {
                        val dmCases = dmUseCases ?: continue

                        // 1) 사용자 존재 확인
                        Log.d("AddMemberViewModel", "사용자 존재 확인: ${memberId.value}")
                        val userCases = userUseCases ?: continue
                        val userExistsResult =
                            userCases.getUserByIdUseCase(DocumentId.from(memberId))
                        when (userExistsResult) {
                            is CustomResult.Success -> {
                                Log.d(
                                    "AddMemberViewModel",
                                    "사용자 존재 확인됨: ${userExistsResult.data.name.value}"
                                )
                            }

                            is CustomResult.Failure -> {
                                Log.e(
                                    "AddMemberViewModel",
                                    "사용자를 찾을 수 없음 - memberId: ${memberId.value}"
                                )
                                failureCount++
                                continue
                            }

                            else -> {
                                Log.e(
                                    "AddMemberViewModel",
                                    "사용자 확인 중 오류 - memberId: ${memberId.value}"
                                )
                                failureCount++
                                continue
                            }
                        }

                        // 2) DM 채널 존재 확인
                        Log.d("AddMemberViewModel", "DM 채널 존재 확인: ${memberId.value}")
                        val channelExistsResult =
                            dmCases.checkDmChannelExistsUseCase(memberId.value)

                        val channelId = when (channelExistsResult) {
                            is CustomResult.Success -> {
                                if (channelExistsResult.data != null) {
                                    // 채널 존재
                                    Log.d(
                                        "AddMemberViewModel",
                                        "기존 DM 채널 사용: ${channelExistsResult.data}"
                                    )
                                    ChannelId(channelExistsResult.data!!)
                                } else {
                                    // 채널 없음 - 새로 생성
                                    Log.d(
                                        "AddMemberViewModel",
                                        "DM 채널 생성 필요 - memberId: ${memberId.value}"
                                    )

                                    // Loading 상태를 제외한 실제 결과 대기
                                    var createResult: CustomResult<DocumentId, Exception>? = null
                                    dmCases.addDmChannelUseCase(memberId.value).collect { result ->
                                        Log.d("AddMemberViewModel", "DM 채널 생성 결과: $result")
                                        when (result) {
                                            is CustomResult.Loading -> {
                                                Log.d("AddMemberViewModel", "DM 채널 생성 중...")
                                                // Loading 상태는 무시하고 계속 대기
                                            }

                                            is CustomResult.Success, is CustomResult.Failure -> {
                                                createResult = result
                                                return@collect // collect 종료
                                            }

                                            else -> {
                                                Log.w(
                                                    "AddMemberViewModel",
                                                    "예상치 못한 DM 채널 생성 상태: $result"
                                                )
                                                createResult =
                                                    CustomResult.Failure(Exception("Unexpected result type: ${result::class.simpleName}"))
                                                return@collect
                                            }
                                        }
                                    }

                                    when (createResult) {
                                        is CustomResult.Success -> {
                                            Log.d(
                                                "AddMemberViewModel",
                                                "DM 채널 생성 성공: ${(createResult as CustomResult.Success<DocumentId>).data.value}"
                                            )
                                            ChannelId.from((createResult as CustomResult.Success<DocumentId>).data)
                                        }

                                        is CustomResult.Failure -> {
                                            Log.e(
                                                "AddMemberViewModel",
                                                "DM 채널 생성 실패: ${(createResult as CustomResult.Failure<Exception>).error.message}"
                                            )
                                            failureCount++
                                            continue
                                        }

                                        null -> {
                                            Log.e("AddMemberViewModel", "DM 채널 생성 결과를 받지 못했습니다")
                                            failureCount++
                                            continue
                                        }

                                        else -> {
                                            Log.e(
                                                "AddMemberViewModel",
                                                "DM 채널 생성 중 알 수 없는 오류: $createResult"
                                            )
                                            failureCount++
                                            continue
                                        }
                                    }
                                }
                            }

                            is CustomResult.Failure -> {
                                Log.e(
                                    "AddMemberViewModel",
                                    "DM 채널 확인 실패: ${channelExistsResult.error.message}"
                                )
                                failureCount++
                                continue
                            }

                            else -> {
                                Log.e("AddMemberViewModel", "DM 채널 확인 중 알 수 없는 오류")
                                failureCount++
                                continue
                            }
                        }

                        // 3) 프로젝트 초대 메시지 전송 (단순화된 UseCase 사용)
                        Log.d("AddMemberViewModel", "프로젝트 초대 메시지 전송 - channelId: $channelId")
                        val messageUseCases = projectMemberUseCases ?: continue

                        Log.d("AddMemberViewModel", "🔥 UseCase 호출 직전 - memberId: ${memberId.value}")

                        try {
                            messageUseCases.sendProjectInviteMessageUseCase(
                                channelId,
                                projectId,
                                memberId
                            )
                                .collect { result ->
                                    Log.d(
                                        "AddMemberViewModel",
                                        "🔥 collect 콜백 실행됨 - result: $result"
                                    )
                                    when (result) {
                                        is CustomResult.Success -> {
                                            Log.d(
                                                "AddMemberViewModel",
                                                "초대 메시지 전송 성공 - memberId: ${memberId.value}"
                                            )
                                            successCount++
                                        }

                                        is CustomResult.Failure -> {
                                            Log.e(
                                                "AddMemberViewModel",
                                                "초대 메시지 전송 실패 - memberId: ${memberId.value}, error: ${result.error.message}"
                                            )
                                            failureCount++
                                        }

                                        is CustomResult.Loading -> {
                                            Log.d(
                                                "AddMemberViewModel",
                                                "초대 메시지 전송 중... - memberId: ${memberId.value}"
                                            )
                                        }

                                        else -> {
                                            Log.w("AddMemberViewModel", "알 수 없는 결과 타입: $result")
                                        }
                                    }
                                }
                            Log.d(
                                "AddMemberViewModel",
                                "🔥 collect 완료 - memberId: ${memberId.value}"
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "AddMemberViewModel",
                                "🔥 UseCase 호출 중 예외 - memberId: ${memberId.value}",
                                e
                            )
                            failureCount++
                        }

                    } catch (e: Exception) {
                        Log.e(
                            "AddMemberViewModel",
                            "멤버 초대 중 예외 발생 - memberId: ${memberId.value}",
                            e
                        )
                        failureCount++
                    }
                }
            } catch (e: Exception) {
                Log.e("AddMemberViewModel", "전체 초대 프로세스 중 예외 발생", e)
                _uiState.update {
                    it.copy(
                        isLoadingFriends = false,
                        error = "초대 중 오류 발생: ${e.message}"
                    )
                }
                return@launch
            }

            Log.d("AddMemberViewModel", "초대 프로세스 완료 - 성공: $successCount, 실패: $failureCount")
            _uiState.update {
                it.copy(
                    isLoadingFriends = false,
                    addSuccess = failureCount == 0,
                    selectedMembers = emptySet() // 선택 초기화
                )
            }

            if (successCount > 0) {
                Log.i("AddMemberViewModel", "초대 성공 - ${successCount}명에게 DM 초대 완료")
                _eventFlow.emit(AddMemberDialogEvent.ShowSnackbar("${successCount}명에게 DM 초대를 보냈습니다."))
                _eventFlow.emit(AddMemberDialogEvent.MembersAddedSuccessfully)
            }
            if (failureCount > 0) {
                Log.w("AddMemberViewModel", "초대 실패 - ${failureCount}명의 초대 실패")
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
