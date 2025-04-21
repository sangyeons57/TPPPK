package com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class AddFriendUiState(
    val username: String = "", // 친구 요청 보낼 사용자 이름
    val isLoading: Boolean = false,
    val error: String? = null, // 서버 응답 에러 메시지
    val infoMessage: String? = null, // 성공 또는 정보 메시지
    val addFriendSuccess: Boolean = false // 요청 성공 시 다이얼로그 닫기 트리거
)

// --- 이벤트 ---
sealed class AddFriendEvent {
    object DismissDialog : AddFriendEvent()
    data class ShowSnackbar(val message: String) : AddFriendEvent() // 스낵바는 호출 측에서
    object ClearFocus : AddFriendEvent()
}

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val friendRepository: FriendRepository,
    // TODO: private val repository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddFriendEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 사용자 이름 입력 변경 시 호출
     */
    fun onUsernameChange(name: String) {
        _uiState.update {
            it.copy(
                username = name,
                error = null, // 입력 시 에러/정보 메시지 초기화
                infoMessage = null
            )
        }
    }

    /**
     * '요청 보내기' 버튼 클릭 시 호출
     */
    fun sendFriendRequest() {
        val usernameToSend = _uiState.value.username.trim()
        if (usernameToSend.isBlank()) {
            _uiState.update { it.copy(error = "사용자 이름을 입력해주세요.") }
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            _eventFlow.emit(AddFriendEvent.ClearFocus)
            println("ViewModel: Sending friend request to $usernameToSend")

            val result = friendRepository.sendFriendRequest(usernameToSend) // ★ Repository 호출

            if (result.isSuccess) {
                val successMessage = result.getOrThrow() // Repository에서 성공 메시지 반환 가정
                _uiState.update { it.copy(isLoading = false, infoMessage = successMessage, addFriendSuccess = true) }
                _eventFlow.emit(AddFriendEvent.ShowSnackbar(successMessage))
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "친구 요청 실패") }
            }
        }
    }
}