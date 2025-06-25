package com.example.feature_find_password.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.auth.AuthPasswordUseCaseProvider
import com.example.domain.provider.auth.AuthValidationUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 비밀번호 찾기 화면의 UI 상태를 정의하는 데이터 클래스
 */
data class FindPasswordUiState(
    val email: String = "",
    // Removed: authCode, newPassword, newPasswordConfirm, isPasswordVisible, 
    // isEmailVerified, passwordChangeSuccess
    val isEmailSent: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 비밀번호 찾기 화면에서 발생하는 일회성 이벤트를 정의하는 Sealed Class
 */
sealed class FindPasswordEvent {
    /**
     * 스낵바 메시지 표시 이벤트
     * @param message 표시할 메시지
     */
    data class ShowSnackbar(val message: String) : FindPasswordEvent()
}

/**
 * 비밀번호 찾기 화면의 비즈니스 로직을 처리하는 ViewModel
 */
@HiltViewModel
class FindPasswordViewModel @Inject constructor(
    private val authPasswordUseCaseProvider: AuthPasswordUseCaseProvider,
    private val authValidationUseCaseProvider: AuthValidationUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹들
    private val authPasswordUseCases = authPasswordUseCaseProvider.create()
    private val authValidationUseCases = authValidationUseCaseProvider.create()

    private val _uiState = MutableStateFlow(FindPasswordUiState())
    val uiState: StateFlow<FindPasswordUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<FindPasswordEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 이메일 입력값 변경 처리
     * @param email 변경된 이메일 값
     */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    // Removed: onAuthCodeChange, onNewPasswordChange, onNewPasswordConfirmChange, onPasswordVisibilityToggle

    /**
     * 비밀번호 재설정 이메일 요청 버튼 클릭 처리
     * 이메일 유효성 검사 후 비밀번호 재설정 이메일 요청 UseCase 실행
     */
    fun requestPasswordResetEmail() { // Renamed from onRequestAuthCodeClick
        val email = _uiState.value.email
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "올바른 이메일 형식이 아닙니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // RequestPasswordResetUseCase 호출
            val result = authPasswordUseCases.requestPasswordResetUseCase(email)

            result.onSuccess {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = false, isEmailSent = true) }
                    _eventFlow.emit(FindPasswordEvent.ShowSnackbar("이메일이 전송되었습니다.")) // Updated message
                }
            }.onFailure { exception ->
                viewModelScope.launch {
                    // Exception을 AuthErrorType.RESET_PASSWORD_FAILURE로 처리하거나 구체적인 오류 메시지 생성
                    _uiState.update { it.copy(isLoading = false, errorMessage = authValidationUseCases.getAuthErrorMessageUseCase(exception)) }
                }
            }
        }
    }

    // Removed: onConfirmAuthCodeClick, onCompletePasswordChangeClick

    /**
     * "완료" 버튼 클릭 처리 (이메일 전송 후)
     */
    fun onDoneClicked() {
        navigationManger.navigateBack()
    }

    /**
     * 돌아가기 버튼 클릭 처리
     */
    fun onBackClick() {
        navigationManger.navigateBack()
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) } // Implemented
    }
}