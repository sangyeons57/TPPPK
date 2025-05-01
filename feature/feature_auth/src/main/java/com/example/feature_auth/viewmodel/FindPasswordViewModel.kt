package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// 비밀번호 찾기 UI 상태
data class FindPasswordUiState(
    val email: String = "",
    val authCode: String = "",
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val isPasswordVisible: Boolean = false, // 새 비밀번호 보이기/숨기기
    val isEmailSent: Boolean = false, // 인증번호 전송 완료 여부
    val isEmailVerified: Boolean = false, // 이메일(인증번호) 인증 완료 여부
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordChangeSuccess: Boolean = false // 비밀번호 변경 성공 여부
)

// 비밀번호 찾기 관련 이벤트
sealed class FindPasswordEvent {
    object NavigateBack : FindPasswordEvent() // 이전 화면으로 돌아가기
    // data class NavigateToLogin(val email: String?) : FindPasswordEvent() // 성공 후 로그인 화면 이동 등
    data class ShowSnackbar(val message: String) : FindPasswordEvent()
}


@HiltViewModel
class FindPasswordViewModel @Inject constructor(
    // TODO: private val authRepository: AuthRepository // 실제 Repository 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(FindPasswordUiState())
    val uiState: StateFlow<FindPasswordUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<FindPasswordEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // --- 입력 값 변경 처리 ---
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onAuthCodeChange(code: String) {
        _uiState.update { it.copy(authCode = code, errorMessage = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    fun onNewPasswordConfirmChange(passwordConfirm: String) {
        _uiState.update { it.copy(newPasswordConfirm = passwordConfirm, errorMessage = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // --- 버튼 클릭 처리 ---
    fun onSendAuthCodeClick() {
        val email = _uiState.value.email
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "올바른 이메일 주소를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 인증번호 전송 요청 - $email")
            // --- TODO: 실제 인증번호 전송 로직 (authRepository.requestPasswordResetCode(email)) ---
            kotlinx.coroutines.delay(1000) // 임시 딜레이
            val success = true // 임시 성공
            // val result = authRepository.requestPasswordResetCode(email)
            // result.onSuccess { ... }.onFailure { ... }
            // -----------------------------------------------------------------------------
            if (success) {
                _uiState.update { it.copy(isLoading = false, isEmailSent = true) }
                _eventFlow.emit(FindPasswordEvent.ShowSnackbar("인증번호가 전송되었습니다."))
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "인증번호 전송에 실패했습니다.") }
            }
        }
    }

    fun onConfirmAuthCodeClick() {
        val email = _uiState.value.email
        val code = _uiState.value.authCode
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "인증번호를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 인증번호 확인 요청 - $code")
            // --- TODO: 실제 인증번호 확인 로직 (authRepository.verifyPasswordResetCode(email, code)) ---
            kotlinx.coroutines.delay(1000) // 임시 딜레이
            val success = true // 임시 성공
            // val result = authRepository.verifyPasswordResetCode(email, code)
            // result.onSuccess { ... }.onFailure { ... }
            // -----------------------------------------------------------------------------
            if (success) {
                _uiState.update { it.copy(isLoading = false, isEmailVerified = true) }
                _eventFlow.emit(FindPasswordEvent.ShowSnackbar("인증되었습니다. 새 비밀번호를 입력하세요."))
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "인증번호가 올바르지 않습니다.") }
            }
        }
    }

    fun onChangePasswordClick() {
        val state = _uiState.value
        if (state.newPassword.isBlank() || state.newPasswordConfirm.isBlank()) {
            _uiState.update { it.copy(errorMessage = "새 비밀번호를 모두 입력해주세요.") }
            return
        }
        if (state.newPassword != state.newPasswordConfirm) {
            _uiState.update { it.copy(errorMessage = "새 비밀번호가 일치하지 않습니다.") }
            return
        }
        // TODO: 비밀번호 유효성 검사 (길이, 특수문자 등) 추가 가능

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            println("ViewModel: 비밀번호 변경 요청")
            // --- TODO: 실제 비밀번호 변경 로직 (authRepository.resetPassword(state.email, state.authCode, state.newPassword)) ---
            kotlinx.coroutines.delay(1000) // 임시 딜레이
            val success = true // 임시 성공
            // val result = authRepository.resetPassword(state.email, state.authCode, state.newPassword)
            // result.onSuccess { ... }.onFailure { ... }
            // -----------------------------------------------------------------------------
            if (success) {
                _uiState.update { it.copy(isLoading = false, passwordChangeSuccess = true) }
                _eventFlow.emit(FindPasswordEvent.ShowSnackbar("비밀번호가 성공적으로 변경되었습니다."))
                _eventFlow.emit(FindPasswordEvent.NavigateBack) // 성공 후 이전 화면으로 이동
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "비밀번호 변경에 실패했습니다.") }
            }
        }
    }

    fun errorMessageShown() {

    }
}