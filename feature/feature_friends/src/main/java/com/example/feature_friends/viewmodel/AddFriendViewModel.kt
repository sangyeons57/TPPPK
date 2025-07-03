package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.domain.model.vo.user.UserName
import com.example.domain.provider.friend.FriendUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

// --- UI 상태 ---
data class AddFriendUiState(
    val username: UserName = UserName.EMPTY, // 친구 요청 보낼 사용자 이름
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
    private val friendUseCaseProvider: FriendUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val authUtil: AuthUtil
) : ViewModel() {

    private val TAG = "AddFriendViewModel"

    // Provider를 통해 생성된 UseCase 그룹들
    private val friendUseCases = friendUseCaseProvider.createForCurrentUser()
    private val userUseCases = userUseCaseProvider.createForUser()

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
                username = UserName(name),
                error = null,
                infoMessage = null,
                addFriendSuccess = false
            )
        }
    }

    /**
     * '요청 보내기' 버튼 클릭 시 호출
     */
    fun sendFriendRequest() {
        val usernameToSearch = _uiState.value.username.trim()
        Log.d(TAG, "sendFriendRequest called with usernameToSearch='$usernameToSearch'")
        
        if (!friendUseCases.validateSearchQueryUseCase(usernameToSearch.value)) {
            Log.d(TAG, "Validation failed for usernameToSearch='$usernameToSearch'")
            _uiState.update { it.copy(error = "유효한 사용자 이름을 입력해주세요 (2글자 이상).") }
            return
        }
        
        if (_uiState.value.isLoading) {
            Log.d(TAG, "Duplicate click ignored – friend request already in progress")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Starting friend request flow…")
            _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            _eventFlow.emit(AddFriendEvent.ClearFocus)

            try {
                val currentUserId = authUtil.getCurrentUserId()
                Log.d(TAG, "Current userId=$currentUserId")
                
                userUseCases.searchUserByNameUseCase(usernameToSearch).collect { userResult ->
                    when(userResult) {
                        is CustomResult.Success -> {
                            val user = userResult.data
                            Log.d(TAG, "User found: id=${user.id.value}, name=${user.name.value}")

                            if (user.id.value == currentUserId) {
                                Log.d(TAG, "Attempted to add self as friend – aborting")
                                _uiState.update { it.copy(
                                    isLoading = false,
                                    error = "자기 자신에게는 친구 요청을 보낼 수 없습니다."
                                )}
                                return@collect
                            }

                            val requestResult = friendUseCases.sendFriendRequestUseCase(user.name.value)
                            Log.d(TAG, "Sending friend request to ${user.name.value}")
                            
                            handleFriendRequestResult(requestResult, user.name.value)
                        }
                        is CustomResult.Failure -> {
                            Log.d(TAG, "User not found: $usernameToSearch")
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "사용자를 찾을 수 없습니다: $usernameToSearch"
                            )}
                        }
                        else -> {
                            Log.d(TAG, "Unhandled user search result: $userResult")
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "처리 중 오류가 발생했습니다."
                            )}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during sendFriendRequest", e)
                _uiState.update { it.copy(
                    isLoading = false, 
                    error = e.message ?: "사용자 검색 중 오류 발생"
                )}
            }
        }
    }
    
    private suspend fun handleFriendRequestResult(
        requestResult: CustomResult<Unit, Exception>,
        targetUsername: String
    ) {
        when (requestResult) {
            is CustomResult.Success -> {
                Log.d(TAG, "Friend request success to $targetUsername")
                _uiState.update { it.copy(
                    isLoading = false,
                    infoMessage = "${targetUsername}님에게 친구 요청을 보냈습니다.",
                    addFriendSuccess = true
                )}
                _eventFlow.emit(AddFriendEvent.ShowSnackbar("친구 요청을 보냈습니다."))
            }
            is CustomResult.Failure -> {
                Log.d(TAG, "Friend request failure: ${requestResult.error}")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = requestResult.error.message ?: "친구 요청 실패"
                )}
            }
            else -> {
                Log.d(TAG, "Unhandled friend request result")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "처리 중 오류가 발생했습니다."
                )}
            }
        }
    }
}