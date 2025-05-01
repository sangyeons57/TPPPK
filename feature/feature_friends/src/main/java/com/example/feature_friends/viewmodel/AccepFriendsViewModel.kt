package com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.FriendRequest
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 모델 (Domain 모델과 동일하다면 생략 가능) ---
data class FriendRequestItem( // 기존 FriendRequest와 동일
    val userId: String,
    val userName: String,
    val profileImageUrl: String?
)
// Domain -> UI 모델 변환
fun FriendRequest.toUiModel(): FriendRequestItem {
    return FriendRequestItem(this.userId, this.userName, this.profileImageUrl)
}

// --- UI 상태 ---
data class AcceptFriendsUiState(
    val friendRequests: List<FriendRequestItem> = emptyList(), // UI 모델 사용
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 (기존과 동일) ---
sealed class AcceptFriendsEvent {
    data class ShowSnackbar(val message: String) : AcceptFriendsEvent()
}


@HiltViewModel
class AcceptFriendsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val friendRepository: FriendRepository,
    // TODO: private val repository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcceptFriendsUiState(isLoading = true))
    val uiState: StateFlow<AcceptFriendsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AcceptFriendsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadFriendRequests()
    }

    /**
     * 친구 요청 목록 로드
     */
    private fun loadFriendRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading friend requests")
            val result = friendRepository.getFriendRequests() // ★ Repository 호출

            if (result.isSuccess) {
                val requests = result.getOrThrow().map { it.toUiModel() } // Domain -> UI 모델 변환
                _uiState.update { it.copy(isLoading = false, friendRequests = requests) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "친구 요청 목록 로드 실패") }
            }
        }
    }

    /**
     * 친구 요청 수락
     */

    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            val currentRequests = _uiState.value.friendRequests // UI 업데이트 전 상태 저장
            // 낙관적 업데이트: UI에서 즉시 제거
            _uiState.update { state ->
                state.copy(friendRequests = state.friendRequests.filterNot { it.userId == userId })
            }

            val result = friendRepository.acceptFriendRequest(userId) // ★ Repository 호출

            if (result.isSuccess) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 수락했습니다."))
                // 성공 시 이미 UI 업데이트 됨
            } else {
                // 실패 시: UI 복구 및 스낵바 표시
                _uiState.update { it.copy(friendRequests = currentRequests) } // UI 복구
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("요청 수락 실패: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    /**
     * 친구 요청 거절
     */
    fun denyFriendRequest(userId: String) {
        viewModelScope.launch {
            val currentRequests = _uiState.value.friendRequests
            _uiState.update { state ->
                state.copy(friendRequests = state.friendRequests.filterNot { it.userId == userId })
            }

            val result = friendRepository.denyFriendRequest(userId) // ★ Repository 호출

            if (result.isSuccess) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 거절했습니다."))
            } else {
                _uiState.update { it.copy(friendRequests = currentRequests) } // UI 복구
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("요청 거절 실패: ${result.exceptionOrNull()?.message}"))
            }
        }
    }
}