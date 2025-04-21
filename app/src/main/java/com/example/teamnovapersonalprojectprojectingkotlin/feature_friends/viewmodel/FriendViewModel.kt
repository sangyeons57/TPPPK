package com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.Friend
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.FriendRepository
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.ui.FriendListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 데이터 모델 ---
data class FriendItem(
    val userId: String,
    val userName: String,
    val status: String, // 예: "온라인", "오프라인"
    val profileImageUrl: String?
)
fun Friend.toUiModel(): FriendItem {
    return FriendItem(this.userId, this.userName, this.status, this.profileImageUrl)
}

// --- UI 상태 ---
data class FriendsListUiState(
    val friends: List<FriendItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class FriendsEvent {
    object NavigateToAcceptFriends : FriendsEvent() // 친구 수락 화면으로 이동
    data class NavigateToChat(val channelId: String) : FriendsEvent() // 친구와의 DM 채팅방으로 이동
    object ShowAddFriendDialog : FriendsEvent() // 친구 추가 다이얼로그 표시
    data class ShowSnackbar(val message: String) : FriendsEvent()
}

@HiltViewModel
class FriendsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val friendRepository: FriendRepository
    // TODO: private val repository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsListUiState(isLoading = true))
    val uiState: StateFlow<FriendsListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<FriendsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadFriendsList()
        refreshFriendsList()
    }


    private fun observeFriendsList() {
        viewModelScope.launch {
            friendRepository.getFriendsListStream()
                .map { domainFriends -> domainFriends.map { it.toUiModel() } } // Domain -> UI 모델 변환
                .catch { e -> _uiState.update { it.copy(error = "친구 목록 오류: ${e.message}", isLoading = false) } }
                .collect { uiFriends ->
                    _uiState.update { it.copy(friends = uiFriends, isLoading = false, error = null) }
                }
        }
    }

    fun refreshFriendsList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendRepository.fetchFriendsList() // ★ Repository 호출
            if (result.isFailure) {
                _uiState.update { it.copy(error = "친구 목록 새로고침 실패", isLoading = false) }
            }
            // 성공 시 Flow가 처리, 로딩 상태만 해제
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 친구 목록 로드
     */
    fun loadFriendsList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading friends list")
            // --- TODO: 실제 친구 목록 로드 (repository.getFriendsList()) ---
            delay(500) // 임시 딜레이
            val success = true
            // val result = repository.getFriendsList()
            // ---------------------------------------------------------
            if (success /*result.isSuccess*/) {
                // 임시 데이터
                val friends = listOf(
                    FriendItem("u1", "김친구", "온라인", null),
                    FriendItem("u2", "이동료", "오프라인", "https://example.com/lee.jpg"),
                    FriendItem("u3", "박선배", "다른 용무 중", null),
                    FriendItem("u4", "최후배", "온라인", null)
                )
                // val friends = result.getOrThrow()
                _uiState.update { it.copy(isLoading = false, friends = friends) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "친구 목록을 불러오지 못했습니다." // result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    /**
     * 친구 아이템 클릭 시 호출 (DM 채팅방으로 이동)
     */
    fun onFriendClick(userId: String) {
        viewModelScope.launch {
            val result = friendRepository.getDmChannelId(userId) // ★ Repository 호출
            if (result.isSuccess) {
                _eventFlow.emit(FriendsEvent.NavigateToChat(result.getOrThrow()))
            } else {
                _eventFlow.emit(FriendsEvent.ShowSnackbar("채팅방 정보를 가져올 수 없습니다."))
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
    fun requestAddFriendDialog() {
        viewModelScope.launch {
            _eventFlow.emit(FriendsEvent.ShowAddFriendDialog)
        }
    }

    // TODO: 친구 추가 다이얼로그에서 친구 추가 요청 처리 함수
    // fun addFriend(username: String) { ... }
}