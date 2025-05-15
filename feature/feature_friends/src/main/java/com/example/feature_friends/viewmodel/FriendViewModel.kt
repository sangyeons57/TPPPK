package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.FriendRelationship
import com.example.domain.model.FriendRequestStatus
import com.example.domain.usecase.friend.GetDmChannelIdUseCase
import com.example.domain.usecase.friend.GetFriendRelationshipsStreamUseCase
import com.example.domain.usecase.friend.RefreshFriendRelationshipsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// --- 데이터 모델 ---
data class FriendItem(
    val friendId: String,
    val status: FriendRequestStatus,
    val relationshipTimestamp: Instant,
    val acceptedAt: Instant?,
    val displayName: String // UI에 표시될 이름 (실제로는 User 정보와 조합 필요)
)

fun FriendRelationship.toUiModel(): FriendItem {
    return FriendItem(
        friendId = this.friendUserId,
        status = this.status,
        relationshipTimestamp = this.timestamp,
        acceptedAt = this.acceptedAt,
        displayName = "Friend: ${this.friendUserId}" // 임시 표시 이름
    )
}

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
    private val getFriendRelationshipsStreamUseCase: GetFriendRelationshipsStreamUseCase,
    private val refreshFriendRelationshipsUseCase: RefreshFriendRelationshipsUseCase,
    private val getDmChannelIdUseCase: GetDmChannelIdUseCase
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
            getFriendRelationshipsStreamUseCase()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .map { result: Result<List<FriendRelationship>> ->
                    result.map { relationships -> relationships.map { it.toUiModel() } }
                }
                .catch { e -> 
                    val errorMessage = e.localizedMessage ?: "알 수 없는 스트림 오류"
                    _uiState.update { it.copy(error = "목록 로딩 중 오류: $errorMessage", isLoading = false) }
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 로딩 실패: $errorMessage"))
                }
                .collect { uiResult: Result<List<FriendItem>> ->
                    if (uiResult.isSuccess) {
                        _uiState.update {
                            it.copy(friends = uiResult.getOrThrow(), isLoading = false, error = null)
                        }
                    } else {
                        val errorMessage = uiResult.exceptionOrNull()?.localizedMessage ?: "알 수 없는 오류"
                        _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                        _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 업데이트 실패: $errorMessage"))
                    }
                }
        }
    }

    /**
     * 친구 목록을 수동으로 새로고침합니다. (선택적 기능)
     * Firestore 리스너가 실시간 업데이트를 제공하므로 필수는 아닐 수 있습니다.
     */
    fun refreshFriendRelationships() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = refreshFriendRelationshipsUseCase()
            if (result.isFailure) {
                val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "알 수 없는 오류"
                _uiState.update { it.copy(error = "새로고침 실패: $errorMessage", isLoading = false) }
                _eventFlow.emit(FriendsEvent.ShowSnackbar("새로고침 실패: $errorMessage"))
            } else {
                _uiState.update { it.copy(isLoading = false, error = null) } 
            }
        }
    }

    /**
     * 친구 아이템 클릭 시 호출 (DM 채팅방으로 이동)
     */
    fun onFriendClick(friendId: String) {
        viewModelScope.launch {
            val result = getDmChannelIdUseCase(friendId)
            if (result.isSuccess) {
                _eventFlow.emit(FriendsEvent.NavigateToChat(result.getOrThrow()))
            } else {
                val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "알 수 없는 오류"
                _eventFlow.emit(FriendsEvent.ShowSnackbar("채팅방 정보를 가져올 수 없습니다: $errorMessage"))
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