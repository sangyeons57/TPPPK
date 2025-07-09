package com.example.feature_friends.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.base.User
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.friend.FriendUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

// --- 데이터 모델 ---
data class FriendItem(
    val user: User?,
    val friendId: UserId,
    val status: FriendStatus,
    val profileImageUrl: ImageUrl?,
    val requestedAt: Instant?,
    val acceptedAt: Instant?,
    val displayName: UserName // UI에 표시될 이름 (실제로는 User 정보와 조합 필요)
)

// --- UI 상태 ---
data class FriendsListUiState(
    val friends: List<FriendItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddFriendDialog: Boolean = false,
    val selectedFriend: FriendItem? = null, // 선택된 친구 (관리 다이얼로그용)
    val showFriendManagementDialog: Boolean = false
)

// --- 이벤트 ---
sealed class FriendsEvent {
    data class ShowSnackbar(val message: String) : FriendsEvent()
}

@HiltViewModel
class FriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val friendUseCaseProvider: FriendUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val authUtil: AuthUtil,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹들
    private lateinit var friendUseCases: com.example.domain.provider.friend.FriendUseCases
    private val userUseCases = userUseCaseProvider.createForUser()
    private lateinit var dmUseCases: com.example.domain.provider.dm.DMUseCases

    // 현재 사용자 ID
    private var currentUserId: String = ""
        set(value) {
            field = value
            if (value.isNotBlank()) {
                dmUseCases = dmUseCaseProvider.createForUser(UserId(value))
            }
        }

    private val _uiState = MutableStateFlow(FriendsListUiState(isLoading = true))
    val uiState: StateFlow<FriendsListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<FriendsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // Initialize with current user ID
        currentUserId = authUtil.getCurrentUserId()
        
        viewModelScope.launch {
            friendUseCases = friendUseCaseProvider.createForCurrentUser()
            observeFriendRelationships()
        }
    }

    /**
     * 친구 목록 실시간 스트림을 구독하고 UI 상태를 업데이트합니다.
     */
    private fun observeFriendRelationships() {
        if (!::friendUseCases.isInitialized) return
        
        Log.d("FriendViewModel", "1")
        viewModelScope.launch {
            Log.d("FriendViewModel", "2")
            friendUseCases.getFriendsListStreamUseCase()
                .onStart {
                    Log.d("FriendViewModel", "3")
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
                .catch { e ->
                    Log.d("FriendViewModel", "4")
                    val errorMessage = e.localizedMessage ?: "알 수 없는 스트림 오류"
                    _uiState.update {
                        it.copy(
                            error = "목록 로딩 중 오류: $errorMessage",
                            isLoading = false
                        )
                    }
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 로딩 실패: $errorMessage"))
                    Log.d("FriendViewModel", "4")
                }
                .collect { result ->
                    Log.d("FriendViewModel", "5")
                    when(result) {
                        is CustomResult.Success -> {
                            Log.d("FriendViewModel", "6")
                            val friends = result.data
                            // 병렬로 각 친구에 대해 사용자 정보 조회
                            val friendItems = friends.map { friend ->
                                async {
                                    Log.d("FriendViewModel", "Loading user data for friend: ${friend.id}")
                                    val userResult =
                                        userUseCases.getUserStreamUseCase(friend.id).first()
                                    val user = when (userResult) {
                                        is CustomResult.Success -> userResult.data
                                        else -> {
                                            Log.w("FriendViewModel", "Failed to load user data for friend: ${friend.id}")
                                            null
                                        }
                                    }

                                    if (user != null) {
                                        FriendItem(
                                            user = user,
                                            friendId = UserId.from(friend.id),
                                            status = friend.status,
                                            profileImageUrl = friend.profileImageUrl,
                                            requestedAt = friend.requestedAt,
                                            acceptedAt = friend.acceptedAt,
                                            displayName = user.name
                                        )
                                    } else null
                                }
                            }.awaitAll().filterNotNull()
                            
                            _uiState.update {
                                it.copy(
                                    friends = friendItems,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is CustomResult.Failure -> {
                            Log.d("FriendViewModel", "7")
                            val errorMessage = result.error.localizedMessage ?: "알 수 없는 오류"
                            _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                            _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 업데이트 실패: $errorMessage"))
                        }
                        is CustomResult.Loading -> {
                            Log.d("FriendViewModel", "Loading friend relationships...")
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                        is CustomResult.Initial -> {
                            Log.d("FriendViewModel", "Initializing friend relationships...")
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                        is CustomResult.Progress -> {
                            Log.d("FriendViewModel", "Friend relationships progress: ${result.progress}")
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                    }
                }
        }
    }

    /**
     * 친구 목록을 수동으로 새로고침합니다. (선택적 기능)
     * Firestore 리스너가 실시간 업데이트를 제공하므로 필수는 아닐 수 있습니다.
     */
    fun refreshFriendRelationships() {
        // 실시간 스트림 덕분에 새로고침이 필요 없으므로 단순히 observeFriendRelationships()를 다시 호출합니다.
        observeFriendRelationships()
        _eventFlow.tryEmit(FriendsEvent.ShowSnackbar("친구 목록을 새로고침했습니다."))
    }

    /**
     * 친구 아이템 클릭 시 호출 (DM 채팅방으로 이동)
     * DMWrapper를 사용하여 기존 DM이 있으면 바로 이동, 없으면 새로 생성하고 이동
     */
    fun onFriendClick(friendId: UserId) {
        viewModelScope.launch {
            try {
                // 1. 현재 DMWrapper 목록에서 해당 친구와의 DM 찾기
                val dmWrappersResult = dmUseCases.getUserDmWrappersUseCase().first()
                when (dmWrappersResult) {
                    is CustomResult.Success -> {
                        val dmWrappers = dmWrappersResult.data
                        val existingDmWrapper = dmWrappers.find { dmWrapper ->
                            dmWrapper.otherUserId.value == friendId.value
                        }
                        
                        if (existingDmWrapper != null) {
                            // 기존 DM 채널로 이동
                            navigationManger.navigateToChat(existingDmWrapper.id.value)
                        } else {
                            // 새로운 DM 채널 생성
                            createNewDmChannelAndNavigate(friendId)
                        }
                    }
                    is CustomResult.Failure -> {
                        // DMWrapper 목록을 가져올 수 없으면 새로 생성 시도
                        createNewDmChannelAndNavigate(friendId)
                    }
                    else -> {
                        _eventFlow.emit(FriendsEvent.ShowSnackbar("DM 정보를 확인 중입니다..."))
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendViewModel", "Error in onFriendClick", e)
                _eventFlow.emit(FriendsEvent.ShowSnackbar("DM 이동 중 오류가 발생했습니다: ${e.localizedMessage}"))
            }
        }
    }
    
    /**
     * 새로운 DM 채널을 생성하고 이동
     */
    private suspend fun createNewDmChannelAndNavigate(friendId: UserId) {
        // 현재 친구 목록에서 해당 친구의 이름 찾기
        val friend = _uiState.value.friends.find { it.friendId == friendId }
        if (friend == null) {
            _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 정보를 찾을 수 없습니다."))
            return
        }
        
        dmUseCases.addDmChannelUseCase(friend.displayName).collect { result ->
            when (result) {
                is CustomResult.Loading -> {
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("DM 채널을 생성하는 중..."))
                }
                is CustomResult.Success -> {
                    val channelId = result.data
                    navigationManger.navigateToChat(channelId.value)
                }
                is CustomResult.Failure -> {
                    val error = result.error
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("DM 채널 생성 실패: ${error.localizedMessage}"))
                }
                else -> {
                    // 다른 상태 처리 (필요 시)
                }
            }
        }
    }

    /**
     * '친구 요청 수락하기' 버튼 클릭 시 호출
     */
    fun onAcceptFriendClick() {
        navigationManger.navigateToAcceptFriends()
    }

    /**
     * '친구 추가하기' 버튼 클릭 시 호출 (TopAppBar Action)
     */
    fun requestAddFriendToggle() {
        _uiState.update { it.copy(showAddFriendDialog= !uiState.value.showAddFriendDialog) }
    }

    /**
     * 친구 아이템 클릭 시 호출 (친구 관리 다이얼로그 표시)
     */
    fun onFriendItemClick(friend: FriendItem) {
        _uiState.update { 
            it.copy(
                selectedFriend = friend,
                showFriendManagementDialog = true
            ) 
        }
    }

    /**
     * 친구 관리 다이얼로그 닫기
     */
    fun dismissFriendManagementDialog() {
        _uiState.update { 
            it.copy(
                selectedFriend = null,
                showFriendManagementDialog = false
            ) 
        }
    }

    /**
     * 친구 제거
     */
    fun removeFriend(friendId: UserId) {
        viewModelScope.launch {
            try {
                val result = friendUseCases.removeFriendUseCase(friendId.value)
                when (result) {
                    is CustomResult.Success -> {
                        _eventFlow.emit(FriendsEvent.ShowSnackbar("친구를 삭제했습니다."))
                        dismissFriendManagementDialog()
                    }
                    is CustomResult.Failure -> {
                        val error = result.error
                        _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 삭제 실패: ${error.message ?: "알 수 없는 오류"}"))
                    }
                    else -> {
                        // 다른 상태 처리 (필요 시)
                    }
                }
            } catch (e: Exception) {
                _eventFlow.emit(FriendsEvent.ShowSnackbar("오류: ${e.message}"))
            }
        }
    }

}