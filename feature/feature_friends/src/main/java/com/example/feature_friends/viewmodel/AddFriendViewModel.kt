package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.domain.model.base.User
import com.example.domain.usecase.friend.SendFriendRequestUseCase
import com.example.domain.usecase.friend.ValidateSearchQueryUseCase
import com.example.domain.usecase.user.SearchUserByNameUseCase
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
    data class ShowSnackbar(val message: String) : AddFriendEvent()
}

/**
 * AddFriendViewModel: 친구 추가 화면의 비즈니스 로직 관리
 */
@HiltViewModel
class AddFriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val searchUserByNameUseCase: SearchUserByNameUseCase,
    private val validateSearchQueryUseCase: ValidateSearchQueryUseCase,
    private val authUtil: AuthUtil
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
        
        // 검색어 유효성 검사
        if (!validateSearchQueryUseCase(usernameToSearch)) {
            _uiState.update { it.copy(error = "유효한 사용자 이름을 입력해주세요 (2글자 이상).") }
            return
        }
        
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            _eventFlow.emit(AddFriendEvent.ClearFocus)

            try {
                // 사용자 검색 - 이름으로 검색
                val currentUserId = authUtil.getCurrentUserId()
                searchUserByNameUseCase(usernameToSearch).collect{ userResult ->
                    when(userResult) {
                        is CustomResult.Success -> {
                            val user = userResult.data

                            // 결과가 있는 경우
                                // 첫 번째 사용자 결과 사용

                                // 자기 자신에게는 친구 요청을 보낼 수 없음
                                if (user.uid.value == currentUserId) {
                                    _uiState.update { it.copy(
                                        isLoading = false,
                                        error = "자기 자신에게는 친구 요청을 보낼 수 없습니다."
                                    )}
                                }

                                // 친구 요청 보내기
                                val requestResult = sendFriendRequestUseCase(user.uid.value)
                                when (requestResult) {
                                    is CustomResult.Success -> {
                                        _uiState.update { it.copy(
                                            isLoading = false,
                                            infoMessage = "${user.name}님에게 친구 요청을 보냈습니다.",
                                            addFriendSuccess = true
                                        )}
                                        _eventFlow.emit(AddFriendEvent.ShowSnackbar("친구 요청을 보냈습니다."))
                                    }
                                    is CustomResult.Failure -> {
                                        _uiState.update { it.copy(
                                            isLoading = false,
                                            error = requestResult.error.message ?: "친구 요청 실패"
                                        )}
                                    }
                                    else -> {
                                        _uiState.update { it.copy(
                                            isLoading = false,
                                            error = "처리 중 오류가 발생했습니다."
                                        )}
                                    }
                                }
                        }
                        is CustomResult.Failure -> {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "사용자를 찾을 수 없습니다: $usernameToSearch"
                            )}
                        }
                        else -> {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "처리 중 오류가 발생했습니다."
                            )}
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false, 
                    error = e.message ?: "사용자 검색 중 오류 발생"
                )}
            }
        }
    }
}