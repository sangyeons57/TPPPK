package com.example.core_ui.dialogs.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.usecase.dm.CreateDmChannelUseCase
import com.example.domain.usecase.friend.SendFriendRequestUseCase
import com.example.domain.usecase.user.SearchUserByNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
/**
 * DM 추가 다이얼로그의 UI 상태
 */
data class AddDmUserUiState(
    val username: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchResults: List<User> = emptyList(),
    val selectedUserId: String? = null,
    val successMessage: String? = null,
    val shouldDismiss: Boolean = false
)

/**
 * DM 추가 다이얼로그의 이벤트
 */
sealed class AddDmUserEvent {
    data class NavigateToDmChat(val dmChannelId: String) : AddDmUserEvent()
    data class ShowSnackbar(val message: String) : AddDmUserEvent()
    object DismissDialog : AddDmUserEvent()
}

/**
 * DM 추가 다이얼로그 ViewModel
 */
@HiltViewModel
class AddDmUserDialogViewModel @Inject constructor(
    private val searchUserByNameUseCase: SearchUserByNameUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val createDmChannelUseCase: CreateDmChannelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDmUserUiState())
    val uiState: StateFlow<AddDmUserUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddDmUserEvent>()
    val eventFlow: SharedFlow<AddDmUserEvent> = _eventFlow.asSharedFlow()

    /**
     * 사용자 이름 입력값 변경 처리
     */
    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(
            username = username,
            errorMessage = null,
            selectedUserId = null,
            successMessage = null
        ) }
    }

    /**
     * 사용자 프로필을 클릭하여 DM 시작
     */
    fun selectUserAndStartDm(userId: String) {
        _uiState.update { it.copy(selectedUserId = userId) }
        startDmWithUser(userId)
    }

    /**
     * DM 채널 생성 및 시작
     */
    private fun startDmWithUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            createDmChannelUseCase(userId).fold(
                onSuccess = { dmChannel ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            successMessage = "DM 채널에 연결되었습니다.",
                            shouldDismiss = true
                        )
                    }
                    _eventFlow.emit(AddDmUserEvent.NavigateToDmChat(dmChannel.id))
                    Log.d("AddDmUserDialogViewModel", "DM channel created successfully: ${dmChannel.id}")
                },
                onFailure = { error ->
                    val errorMsg = "DM 채널 연결 중 오류: ${error.message ?: "알 수 없는 오류"}"
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                    _eventFlow.emit(AddDmUserEvent.ShowSnackbar(errorMsg))
                    Log.e("AddDmUserDialogViewModel", "Failed to create DM channel: ${error.message}", error)
                }
            )
        }
    }

    /**
     * 검색 버튼 클릭 시
     */
    fun searchUser() {
        val username = _uiState.value.username.trim()
        
        if (username.isBlank()) {
            val errorMsg = "사용자 이름을 입력해주세요."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            viewModelScope.launch { _eventFlow.emit(AddDmUserEvent.ShowSnackbar(errorMsg)) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            // suspend 함수를 코루틴 내에서 한 번만 호출
            searchUserByNameUseCase(username).fold(
                onSuccess = { users ->
                    if (users.isEmpty()) {
                        val errorMsg = "일치하는 사용자를 찾을 수 없습니다."
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorMsg
                            )
                        }
                        _eventFlow.emit(AddDmUserEvent.ShowSnackbar(errorMsg))
                    } else {
                        val firstUser: User = users.first()
                        // 사용자를 찾으면 바로 DM 시작
                        Log.d("AddDmUserDialogViewModel", "User found: $firstUser, starting DM...")
                        // 코루틴 내에서 이미 실행 중이므로 직접 createDmChannelUseCase 호출
                        createDmChannelUseCase(firstUser.uid).fold(
                            onSuccess = { dmChannel ->
                                _uiState.update { state ->
                                    state.copy(
                                        isLoading = false,
                                        successMessage = "DM 채널에 연결되었습니다.",
                                        shouldDismiss = true
                                    )
                                }
                                _eventFlow.emit(AddDmUserEvent.NavigateToDmChat(dmChannel.id))
                                Log.d("AddDmUserDialogViewModel", "DM channel created successfully: ${dmChannel.id}")
                            },
                            onFailure = { error ->
                                val errorMsg = "DM 채널 연결 중 오류: ${error.message ?: "알 수 없는 오류"}"
                                _uiState.update { state ->
                                    state.copy(
                                        isLoading = false,
                                        errorMessage = errorMsg
                                    )
                                }
                                _eventFlow.emit(AddDmUserEvent.ShowSnackbar(errorMsg))
                                Log.e("AddDmUserDialogViewModel", "Failed to create DM channel: ${error.message}", error)
                            }
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = "사용자 검색 중 오류가 발생했습니다: ${error.message ?: "알 수 없는 오류"}"
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                    _eventFlow.emit(AddDmUserEvent.ShowSnackbar(errorMsg))
                }
            )
        }
    }

    /**
     * 다이얼로그 닫기
     */
    fun dismiss() {
        viewModelScope.launch {
            _eventFlow.emit(AddDmUserEvent.DismissDialog)
        }
    }
}
        **/