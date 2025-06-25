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
    val showAddFriendDialog: Boolean = false
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
    private val friendUseCases = friendUseCaseProvider.createForCurrentUser()
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
        observeFriendRelationships()
    }

    /**
     * 친구 목록 실시간 스트림을 구독하고 UI 상태를 업데이트합니다.
     */
    private fun observeFriendRelationships() {
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
                            val friendItems = mutableListOf<FriendItem>()
                            
                            // 각 친구에 대해 사용자 정보 조회
                            for (friend in friends) {
                                Log.d("FriendViewModel", "6.5")
                                val userResult =
                                    userUseCases.getUserStreamUseCase(friend.id).first()
                                val user = when (userResult) {
                                    is CustomResult.Success -> userResult.data
                                    else -> null
                                }

                                if (user != null) {
                                    friendItems.add(FriendItem(
                                        user = user,
                                        friendId = UserId.from(friend.id),
                                        status = friend.status,
                                        profileImageUrl = friend.profileImageUrl,
                                        requestedAt = friend.requestedAt,
                                        acceptedAt = friend.acceptedAt,
                                        displayName = user.name
                                    )
                                    )
                                }
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
                            Log.d("FriendViewModel", "7")
                            val errorMessage = result.error.localizedMessage ?: "알 수 없는 오류"
                            _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                            _eventFlow.emit(FriendsEvent.ShowSnackbar("친구 목록 업데이트 실패: $errorMessage"))
                        }
                        else -> {
                            Log.d("FriendViewModel", "8")
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
    fun onFriendClick(friendId: UserId) {
        viewModelScope.launch {
            val result = dmUseCases.getDmChannelUseCase(friendId.value)
            when (result) {
                is CustomResult.Success -> {
                    val dmChannel = result.data
                    navigationManger.navigateToChat(dmChannel.id.value)
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
        navigationManger.navigateToAcceptFriends()
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