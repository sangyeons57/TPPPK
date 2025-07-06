package com.example.feature_accept_friend.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.domain.provider.friend.FriendUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// UI 모델 (Friend 도메인 모델 기반으로 변경)
data class FriendRequestItem(
    val friendRequestId: UserId, // 친구 요청 ID (accept/reject에 사용)
    val requesterId: UserId, // 요청을 보낸 사용자 ID
    val userName: UserName,
    val profileImageUrl: ImageUrl? = null,
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
}

/**
 * AcceptFriendsViewModel: 친구 요청 수락 화면의 비즈니스 로직 관리
 */
@HiltViewModel
class AcceptFriendsViewModel @Inject constructor(
    private val friendUseCaseProvider: FriendUseCaseProvider,
    private val authUtil: AuthUtil,
    private val navigationManger: NavigationManger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private lateinit var friendUseCases: com.example.domain.provider.friend.FriendUseCases

    // UI 상태 (MutableStateFlow -> StateFlow로 외부 노출)
    private val _uiState = MutableStateFlow(AcceptFriendsUiState())
    val uiState: StateFlow<AcceptFriendsUiState> = _uiState.asStateFlow()

    // 이벤트 관리 (SharedFlow로 일회성 이벤트 방출)
    private val _eventFlow = MutableSharedFlow<AcceptFriendsEvent>()
    val eventFlow: SharedFlow<AcceptFriendsEvent> = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            friendUseCases = friendUseCaseProvider.createForCurrentUser()
            loadPendingFriendRequests()
        }
    }

    /**
     * 받은 친구 요청 목록을 불러옵니다.
     */
    private fun loadPendingFriendRequests() {
        if (!::friendUseCases.isInitialized) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d("AcceptFriendsViewModel", "1")
            try {
                authUtil.getCurrentUserId()
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
                                    friendRequestId = UserId.from(friend.id.value), // 친구 요청 문서 ID
                                    requesterId = UserId.from(friend.id.value), // TODO: 실제 요청자 ID로 수정 필요
                                    userName = friend.name,
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
     * @param userId 수락할 사용자 ID (실제로는 friendRequestId)
     */
    fun acceptFriendRequest(userId: UserId) {
        viewModelScope.launch {
            try {
                authUtil.getCurrentUserId()
                // userId는 실제로 friendRequestId를 의미함
                val result = friendUseCases.acceptFriendRequestUseCase(userId.value)
                when (result) {
                    is CustomResult.Success -> {
                        // 성공 시 목록에서 해당 요청 제거 (UI 상태 업데이트)
                        _uiState.update { currentState ->
                            currentState.copy(
                                friendRequests = currentState.friendRequests.filter { it.friendRequestId != userId }
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
     * @param userId 거절할 사용자 ID (실제로는 friendRequestId)
     */
    fun denyFriendRequest(userId: UserId) {
        viewModelScope.launch {
            try {
                // userId는 실제로 friendRequestId를 의미함
                val result = friendUseCases.rejectFriendRequestUseCase(userId.value)
                when (result) {
                    is CustomResult.Success -> {
                        // 성공 시 목록에서 해당 요청 제거 (UI 상태 업데이트)
                        _uiState.update { currentState ->
                            currentState.copy(
                                friendRequests = currentState.friendRequests.filter { it.friendRequestId != userId }
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

    /**
     * 뒤로가기 네비게이션 처리
     */
    fun navigateBack() {
        navigationManger.navigateBack()
    }
} 