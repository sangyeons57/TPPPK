package com.example.feature_friends.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.domain.model.base.Friend
import com.example.domain.provider.friend.FriendUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// UI 모델 (Friend 도메인 모델 기반으로 변경)
data class FriendRequestItem(
    val userId: String,
    val userName: String,
    val profileImageUrl: String? = null,
    val requestDate: Instant? = null
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
    private val friendUseCaseProvider: FriendUseCaseProvider,
    private val authUtil: AuthUtil,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val friendUseCases = friendUseCaseProvider.createForCurrentUser()

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

            Log.d("AcceptFriendsViewModel", "1")
            try {
                val currentUserId = authUtil.getCurrentUserId()
                Log.d("AcceptFriendsViewModel", "2")
                friendUseCases.getPendingFriendRequestsUseCase().collect { result ->
                    Log.d("AcceptFriendsViewModel", "3")
                    when (result) {
                        is CustomResult.Success -> {
                            Log.d("AcceptFriendsViewModel", "4")
                            val friends = result.data
                            // Friend 객체를 UI 모델로 변환
                            val requests = friends.map { friend ->
                                Log.d("AcceptFriendsViewModel", "5")
                                FriendRequestItem(
                                    userId = friend.id,
                                    userName = friend.name.ifEmpty { "사용자 ${friend.id.takeLast(4)}" },
                                    profileImageUrl = friend.profileImageUrl,
                                    requestDate = friend.requestedAt
                                )
                            }
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    friendRequests = requests
                                )
                            }
                            Log.d("AcceptFriendsViewModel", "6")
                        }
                        is CustomResult.Failure -> {
                            Log.d("AcceptFriendsViewModel", "7")
                            val error = result.error
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "친구 요청을 불러오는데 실패했습니다."
                                )
                            }
                            _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 불러오는데 실패했습니다."))
                            Log.d("AcceptFriendsViewModel", "8")
                        }
                        else -> {
                            // Loading, Initial, Progress 등의 상태 처리 (필요 시)
                            Log.d("AcceptFriendsViewModel", "9")
                            Log.d("AcceptFriendsViewModel", result.toString())
                        }
                    }
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
                val currentUserId = authUtil.getCurrentUserId()
                val result = friendUseCases.acceptFriendRequestUseCase(userId, currentUserId)
                when (result) {
                    is CustomResult.Success -> {
                        // 성공 시 목록에서 해당 요청 제거 (UI 상태 업데이트)
                        _uiState.update { currentState ->
                            currentState.copy(
                                friendRequests = currentState.friendRequests.filter { it.userId != userId }
                            )
                        }
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 수락했습니다."))
                    }
                    is CustomResult.Failure -> {
                        val error = result.error
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청 수락 실패: ${error.message ?: "알 수 없는 오류"}"))
                    }
                    else -> {
                        // 다른 상태 처리 (필요 시)
                    }
                }
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
                val result = friendUseCases.removeOrDenyFriendUseCase(userId)
                when (result) {
                    is CustomResult.Success -> {
                        // 성공 시 목록에서 해당 요청 제거 (UI 상태 업데이트)
                        _uiState.update { currentState ->
                            currentState.copy(
                                friendRequests = currentState.friendRequests.filter { it.userId != userId }
                            )
                        }
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 거절했습니다."))
                    }
                    is CustomResult.Failure -> {
                        val error = result.error
                        _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청 거절 실패: ${error.message ?: "알 수 없는 오류"}"))
                    }
                    else -> {
                        // 다른 상태 처리 (필요 시)
                    }
                }
            } catch (e: Exception) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("오류: ${e.message}"))
            }
        }
    }
} 