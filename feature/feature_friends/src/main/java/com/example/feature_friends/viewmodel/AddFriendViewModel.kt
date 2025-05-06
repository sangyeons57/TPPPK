package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.SearchRepository
import com.example.domain.usecase.friend.SendFriendRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    object ClearFocus : AddFriendEvent()
}

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val searchRepository: SearchRepository // 추가
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddFriendEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 사용자 이름 입력 변경 시 호출
     */
    fun onUsernameChange(name: String) {
        _uiState.update { it.copy(username = name, error = null, infoMessage = null, addFriendSuccess = false) }
    }

    /**
     * '요청 보내기' 버튼 클릭 시 호출
     */
    fun sendFriendRequest() {
        val usernameToSearch = _uiState.value.username.trim()
        if (usernameToSearch.isBlank()) {
            _uiState.update { it.copy(error = "사용자 이름을 입력해주세요.") }
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            _eventFlow.emit(AddFriendEvent.ClearFocus)

            // 1. 사용자 이름으로 UserRepository 또는 SearchRepository를 통해 userId 검색
            // 여기서는 SearchRepository.searchUsers (정확한 닉네임 일치)를 가정합니다.
            // 실제 SearchRepository의 함수 시그니처에 따라 수정 필요.
            val searchResult = searchRepository.searchUsers(query = usernameToSearch) // searchUsers가 Result<List<User>> 등을 반환한다고 가정
            
            if (searchResult.isSuccess) {
                val usersFound = searchResult.getOrNull() // 실제 User 모델 타입 사용
                if (usersFound != null && usersFound.isNotEmpty()) {
                    // 보통 닉네임 검색은 유일한 결과를 기대하거나, 여러 개 중 선택하게 함
                    // 여기서는 첫 번째 사용자를 대상으로 한다고 가정
                    val targetUser = usersFound.first() // 실제 User 모델의 id 필드 사용 (예: targetUser.id)
                    val targetUserId = targetUser.id // User 모델에 id 필드가 있다고 가정

                    // 2. 검색된 userId로 친구 요청 보내기
                    val requestResult = sendFriendRequestUseCase(targetUserId)
                    if (requestResult.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, infoMessage = "친구 요청을 보냈습니다.", addFriendSuccess = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = requestResult.exceptionOrNull()?.message ?: "친구 요청 실패") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "사용자를 찾을 수 없습니다: $usernameToSearch") }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = searchResult.exceptionOrNull()?.message ?: "사용자 검색 중 오류 발생") }
            }
        }
    }
}