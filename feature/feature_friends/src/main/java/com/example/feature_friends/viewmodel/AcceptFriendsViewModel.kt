package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.FriendRelationship
import com.example.domain.usecase.friend.AcceptFriendRequestUseCase
import com.example.domain.usecase.friend.GetPendingFriendRequestsUseCase
import com.example.domain.usecase.friend.RemoveOrDenyFriendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// UI 모델 (FriendRelationship 기반으로 변경)
data class FriendRequestItem(
    val userId: String,
    val userName: String,
    val profileImageUrl: String? = null,
    val requestDate: Date? = null
)

// --- UI 상태 ---
data class AcceptFriendsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val friendRequests: List<FriendRequestItem> = emptyList()
)

// --- UI 이벤트 ---
sealed class AcceptFriendsEvent {
    data class ShowSnackbar(val message: String) : AcceptFriendsEvent()
    data object NavigateBack : AcceptFriendsEvent()
}

/**
 * AcceptFriendsViewModel: 친구 요청 수락 화면의 비즈니스 로직 관리
 */
@HiltViewModel
class AcceptFriendsViewModel @Inject constructor(
    private val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val denyFriendRequestUseCase: RemoveOrDenyFriendUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI 상태 (MutableStateFlow -> StateFlow로 외부 노출)
    private val _uiState = MutableStateFlow(AcceptFriendsUiState())
    val uiState: StateFlow<AcceptFriendsUiState> = _uiState.asStateFlow()

    // 이벤트 관리 (SharedFlow로 일회성 이벤트 방출)
    private val _eventFlow = MutableSharedFlow<AcceptFriendsEvent>()
    val eventFlow: SharedFlow<AcceptFriendsEvent> = _eventFlow.asSharedFlow()

    init {
        loadPendingFriendRequests()
    }

    /**
     * 받은 친구 요청 목록을 불러옵니다.
     */
    fun loadPendingFriendRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                getPendingFriendRequestsUseCase().collect { result ->
                    result.fold(
                        onSuccess = { relationships ->
                            // Firestore에서 추가 정보 조회 필요 - 이름, 프로필 이미지 등 (실제 구현에서는 UserRepository 활용)
                            // 여기서는 더미 데이터 사용
                            val requests = relationships.map { relationship ->
                                FriendRequestItem(
                                    userId = relationship.friendId, // FriendRelationship의 friendId 필드 사용
                                    userName = "사용자 ${relationship.friendId.takeLast(4)}", // 실제 구현에서는 UserRepository에서 이름 가져오기
                                    profileImageUrl = null, // 실제 구현에서는 UserRepository에서 프로필 이미지 URL 가져오기
                                    requestDate = relationship.timestamp
                                )
                            }
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    friendRequests = requests
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "친구 요청을 불러오는데 실패했습니다."
                                )
                            }
                            _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 불러오는데 실패했습니다."))
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("오류: ${e.message}"))
            }
        }
    }

    /**
     * 특정 사용자의 친구 요청을 수락합니다.
     * 
     * @param userId 수락할 사용자 ID
     */
    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                acceptFriendRequestUseCase(userId).fold(
                    onSuccess = {
                        // 성공 시 목록에서 해당 요청 제거 (UI 상태 업데이트)
                        _uiState.update { currentState ->
                            currentState.copy(
                                friendRequests = currentState.friendRequests.filter { it.userId != userId }
                            )
                        }
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 수락했습니다."))
                    },
                    onFailure = { error ->
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청 수락 실패: ${error.message ?: "알 수 없는 오류"}"))
                    }
                )
            } catch (e: Exception) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("오류: ${e.message}"))
            }
        }
    }

    /**
     * 특정 사용자의 친구 요청을 거절합니다.
     * 
     * @param userId 거절할 사용자 ID
     */
    fun denyFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                denyFriendRequestUseCase(userId).fold(
                    onSuccess = {
                        // 성공 시 목록에서 해당 요청 제거 (UI 상태 업데이트)
                        _uiState.update { currentState ->
                            currentState.copy(
                                friendRequests = currentState.friendRequests.filter { it.userId != userId }
                            )
                        }
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 거절했습니다."))
                    },
                    onFailure = { error ->
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청 거절 실패: ${error.message ?: "알 수 없는 오류"}"))
                    }
                )
            } catch (e: Exception) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("오류: ${e.message}"))
            }
        }
    }
} 