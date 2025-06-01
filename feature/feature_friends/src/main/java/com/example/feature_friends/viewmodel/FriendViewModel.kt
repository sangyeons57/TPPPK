package com.example.feature_friends.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.domain.model.base.Friend
import com.example.domain.model.base.User
import com.example.domain.model.enum.FriendStatus
import com.example.domain.usecase.dm.GetDmChannelIdUseCase
import com.example.domain.usecase.friend.GetFriendsListStreamUseCase
import com.example.domain.usecase.user.GetUserUseCase
import com.example.feature_friends.ui.FriendListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.time.Instant
import javax.inject.Inject

// --- 데이터 모델 ---
data class FriendItem(
    val user: User?,
    val friendId: String,
    val status: FriendStatus,
    val profileImageUrl: String?,
    val requestedAt: Instant?,
    val acceptedAt: Instant?,
    val displayName: String // UI에 표시될 이름 (실제로는 User 정보와 조합 필요)
)

// --- UI 상태 ---
data class FriendsListUiState(
    val friends: List<FriendItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddFriendDialog: Boolean = false
)

// --- 이벤트 ---
sealed class FriendsEvent {
    object NavigateToAcceptFriends : FriendsEvent() // 친구 수락 화면으로 이동
    data class NavigateToChat(val channelId: String) : FriendsEvent() // 친구와의 DM 채팅방으로 이동
    data class ShowSnackbar(val message: String) : FriendsEvent()
}

@HiltViewModel
class FriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val getUserUseCase: GetUserUseCase,
    private val getDmChannelIdUseCase: GetDmChannelIdUseCase,
    private val getFriendsListStreamUseCase: GetFriendsListStreamUseCase,
    private val authUtil: AuthUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsListUiState(isLoading = true))
    val uiState: StateFlow<FriendsListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<FriendsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeFriendRelationships()
    }

    /**
     * 친구 목록 실시간 스트림을 구독하고 UI 상태를 업데이트합니다.
     */
    private fun observeFriendRelationships() {
        viewModelScope.launch {
            val currentUserId = authUtil.getCurrentUserId()
            getFriendsListStreamUseCase(currentUserId)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e ->
                    val errorMessage = e.localizedMessage ?: "알 수 없는 스트림 오류"
                    _uiState.update {
                        it.copy(
                            error = "목록 로딩 중 오류: $errorMessage",
                            isLoading = false
                        )
                    }
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 로딩 실패: $errorMessage"))
                }
                .collect { result ->
                    when(result) {
                        is CustomResult.Success -> {
                            val friends = result.data
                            val friendItems = mutableListOf<FriendItem>()
                            
                            // 각 친구에 대해 사용자 정보 조회
                            for (friend in friends) {
                                val userResult = getUserUseCase(friend.friendUid).first()
                                val user = when (userResult) {
                                    is CustomResult.Success -> userResult.data
                                    else -> null
                                }
                                
                                friendItems.add(FriendItem(
                                    user = user,
                                    friendId = friend.friendUid,
                                    status = friend.status,
                                    profileImageUrl = friend.friendProfileImageUrl ?: user?.profileImageUrl,
                                    requestedAt = friend.requestedAt,
                                    acceptedAt = friend.acceptedAt,
                                    displayName = user?.name ?: friend.friendName.ifEmpty { "사용자 ${friend.friendUid.takeLast(4)}" }
                                ))
                            }
                            
                            _uiState.update {
                                it.copy(
                                    friends = friendItems,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is CustomResult.Failure -> {
                            val errorMessage = result.error.localizedMessage ?: "알 수 없는 오류"
                            _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                            _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 업데이트 실패: $errorMessage"))
                        }
                        else -> {
                            // Loading, Initial, Progress 등의 상태 처리 (필요 시)
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
     */
    fun onFriendClick(friendId: String) {
        viewModelScope.launch {
            val result = getDmChannelIdUseCase(friendId)
            when (result) {
                is CustomResult.Success -> {
                    val channelId = result.data
                    if (channelId != null) {
                        _eventFlow.emit(FriendsEvent.NavigateToChat(channelId))
                    } else {
                        _eventFlow.emit(FriendsEvent.ShowSnackbar("채팅방 정보가 없습니다."))
                    }
                }
                is CustomResult.Failure -> {
                    val error = result.error
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("채팅방 정보를 가져올 수 없습니다: $error"))
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
        viewModelScope.launch {
            _eventFlow.emit(FriendsEvent.NavigateToAcceptFriends)
        }
    }

    /**
     * '친구 추가하기' 버튼 클릭 시 호출 (TopAppBar Action)
     */
    fun requestAddFriendToggle() {
        _uiState.update { it.copy(showAddFriendDialog= !uiState.value.showAddFriendDialog) }
    }

    // TODO: 친구 추가 다이얼로그에서 친구 추가 요청 처리 함수
    // fun addFriend(username: String) { ... }
}