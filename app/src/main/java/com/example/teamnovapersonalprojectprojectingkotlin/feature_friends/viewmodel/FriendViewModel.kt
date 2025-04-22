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
    private val friendRepository: FriendRepository
    // TODO: private val repository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsListUiState(isLoading = true))
    val uiState: StateFlow<FriendsListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<FriendsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeFriendsList()
        refreshFriendsList()
    }


    /**
     * 친구 목록 실시간 스트림을 구독하고 UI 상태를 업데이트합니다.
     */
    private fun observeFriendsList() {
        viewModelScope.launch {
            friendRepository.getFriendsListStream() // ★ Repository의 스트림 함수 호출
                .map { domainFriends ->
                    // Domain 모델 리스트 -> UI 모델(FriendItem) 리스트 변환
                    domainFriends.map { it.toUiModel() }
                }
                .onStart {
                    // 스트림 시작 시 로딩 상태 설정 (선택적, 초기 로딩은 이미 true)
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
                .catch { e ->
                    // 스트림 처리 중 에러 발생 시
                    println("Error observing friends list: $e")
                    _uiState.update { it.copy(error = "친구 목록을 불러오는 중 오류가 발생했습니다.", isLoading = false) }
                    _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 로딩 실패"))
                }
                .collect { uiFriends ->
                    // 새 친구 목록 데이터 수신 시 UI 상태 업데이트
                    _uiState.update {
                        it.copy(
                            friends = uiFriends,
                            isLoading = false, // 데이터 수신 완료 시 로딩 해제
                            error = null // 성공 시 에러 메시지 초기화
                        )
                    }
                }
        }
    }

    /**
     * 친구 목록을 수동으로 새로고침합니다. (선택적 기능)
     * Firestore 리스너가 실시간 업데이트를 제공하므로 필수는 아닐 수 있습니다.
     */
    fun refreshFriendsList() {
        viewModelScope.launch {
            // 로딩 상태 표시 (이미 스트림에서 처리 중일 수 있으므로 중복될 수 있음)
            _uiState.update { it.copy(isLoading = true) }
            val result = friendRepository.fetchFriendsList() // 백엔드와 동기화 시도
            if (result.isFailure) {
                _uiState.update { it.copy(error = "친구 목록 새로고침 실패", isLoading = false) }
                _eventFlow.emit(FriendsEvent.ShowSnackbar("새로고침 실패"))
            } else {
                // 성공 시 Firestore 리스너가 자동으로 데이터를 업데이트하므로,
                // 여기서는 로딩 상태만 해제하거나 별도 처리가 필요 없을 수 있습니다.
                // 명시적으로 로딩 상태를 해제합니다.
                _uiState.update { it.copy(isLoading = false) }
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
    fun requestAddFriendToggle() {
        _uiState.update { it.copy(showAddFriendDialog= !uiState.value.showAddFriendDialog) }
    }

    // TODO: 친구 추가 다이얼로그에서 친구 추가 요청 처리 함수
    // fun addFriend(username: String) { ... }
}