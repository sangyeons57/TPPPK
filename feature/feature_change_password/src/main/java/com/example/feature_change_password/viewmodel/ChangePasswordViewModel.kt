package com.example.feature_change_password.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.domain.usecase.auth.RequestPasswordResetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// --- UI 상태 ---
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val currentPasswordError: String? = null, // 현재 비밀번호 에러
    val newPasswordError: String? = null, // 새 비밀번호 에러
    val confirmPasswordError: String? = null, // 확인 비밀번호 에러
    val changeSuccess: Boolean = false // 변경 성공 시 네비게이션 트리거
)

// --- 이벤트 ---
sealed class ChangePasswordEvent {
    object NavigateBack : ChangePasswordEvent()
    data class ShowSnackbar(val message: String) : ChangePasswordEvent()
    object ClearFocus : ChangePasswordEvent()
}


@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 필요 시 사용
    requestPasswordResetUseCase: RequestPasswordResetUseCase,
    // 필요 시 사용
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChangePasswordEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 현재 비밀번호 입력 변경 시 호출
     */
    fun onCurrentPasswordChange(password: String) {
        _uiState.update { it.copy(currentPassword = password, currentPasswordError = null) }
    }

    /**
     * 새 비밀번호 입력 변경 시 호출
     */
    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, newPasswordError = null) }
    }

    /**
     * 새 비밀번호 확인 입력 변경 시 호출
     */
    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, confirmPasswordError = null) }
    }

    /**
     * '변경하기' 버튼 클릭 시 호출
     */
    fun changePassword() {
        val currentState = _uiState.value
        val currentPassword = currentState.currentPassword
        val newPassword = currentState.newPassword
        val confirmPassword = currentState.confirmPassword

        // 유효성 검사
        var hasError = false
        var currentPasswordError: String? = null
        var newPasswordError: String? = null
        var confirmPasswordError: String? = null

        if (currentPassword.isBlank()) {
            currentPasswordError = "현재 비밀번호를 입력해주세요."
            hasError = true
        }
        if (newPassword.length < 6) { // 예: 최소 6자리
            newPasswordError = "새 비밀번호는 6자 이상 입력해주세요."
            hasError = true
        }
        if (newPassword != confirmPassword) {
            confirmPasswordError = "새 비밀번호가 일치하지 않습니다."
            hasError = true
        }
        if (newPassword == currentPassword && newPassword.isNotBlank()) {
            newPasswordError = "현재 비밀번호와 다른 비밀번호를 사용해주세요."
            hasError = true
        }

        _uiState.update {
            it.copy(
                currentPasswordError = currentPasswordError,
                newPasswordError = newPasswordError,
                confirmPasswordError = confirmPasswordError
            )
        }

        if (hasError || currentState.isLoading) return // 에러 있거나 로딩 중이면 중단

        /**
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _eventFlow.emit(ChangePasswordEvent.ClearFocus)
            println("ViewModel: Attempting to change password")

            // 비밀번호 변경 요청
            val result =  reque(currentPassword, newPassword)
            requestPasswordResetUseCase()

            if (result.isSuccess) {
                _eventFlow.emit(ChangePasswordEvent.ShowSnackbar("비밀번호가 변경되었습니다."))
                _uiState.update { it.copy(isLoading = false, changeSuccess = true) } // 성공 및 뒤로가기 트리거
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "비밀번호 변경 실패"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // 특정 필드 에러 또는 일반 에러 표시
                        currentPasswordError = if (errorMessage.contains("현재 비밀번호")) errorMessage else null
                    )
                }
                _eventFlow.emit(ChangePasswordEvent.ShowSnackbar(errorMessage))
            }
        }
        **/
    }
}