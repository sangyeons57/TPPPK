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
data class FriendRequestDisplayItem(
    val friendId: String,
    val status: String,
    val requestTimestamp: Date,
    val displayName: String 
)

fun FriendRelationship.toRequestDisplayItem(): FriendRequestDisplayItem {
    return FriendRequestDisplayItem(
        friendId = this.friendId,
        status = this.status,
        requestTimestamp = this.timestamp,
        displayName = "Request from: ${this.friendId}" 
    )
}

data class AcceptFriendsUiState(
    val friendRequests: List<FriendRequestDisplayItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class AcceptFriendsEvent {
    data class ShowSnackbar(val message: String) : AcceptFriendsEvent()
}

@HiltViewModel
class AcceptFriendsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val removeOrDenyFriendUseCase: RemoveOrDenyFriendUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcceptFriendsUiState(isLoading = true))
    val uiState: StateFlow<AcceptFriendsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AcceptFriendsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadFriendRequests()
    }

    private fun loadFriendRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getPendingFriendRequestsUseCase()
                .map { result: Result<List<FriendRelationship>> ->
                    result.mapCatching { relationships ->
                        relationships.map { it.toRequestDisplayItem() }
                    }
                }
                .catch { e ->
                    val errorMessage = e.localizedMessage ?: "알 수 없는 스트림 오류"
                    _uiState.update { it.copy(isLoading = false, error = "요청 목록 로드 실패: $errorMessage") }
                }
                .collect { result: Result<List<FriendRequestDisplayItem>> ->
                    if (result.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, friendRequests = result.getOrThrow()) }
                    } else {
                        val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "알 수 없는 오류"
                        _uiState.update { it.copy(isLoading = false, error = "요청 목록 로드 실패: $errorMessage") }
                    }
                }
        }
    }

    fun acceptFriendRequest(friendId: String) {
        viewModelScope.launch {
            val result = acceptFriendRequestUseCase(friendId) 
            if (result.isSuccess) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 수락했습니다."))
                loadFriendRequests() // 목록 새로고침
            } else {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("요청 수락 실패: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun denyFriendRequest(friendId: String) {
        viewModelScope.launch {
            val result = removeOrDenyFriendUseCase(friendId) 
            if (result.isSuccess) {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("친구 요청을 거절했습니다."))
                loadFriendRequests() // 목록 새로고침
            } else {
                _eventFlow.emit(AcceptFriendsEvent.ShowSnackbar("요청 거절 실패: ${result.exceptionOrNull()?.message}"))
            }
        }
    }
}