package com.example.feature_auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.auth.GetAuthErrorMessageUseCase
import com.example.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.domain.usecase.auth.ResetPasswordUseCase
import com.example.domain.usecase.auth.VerifyPasswordResetCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 비밀번호 찾기 화면의 UI 상태를 정의하는 데이터 클래스
 */
data class FindPasswordUiState(
    val email: String = "",
    val authCode: String = "",
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val isPasswordVisible: Boolean = false,
    val isEmailSent: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordChangeSuccess: Boolean = false
)

/**
 * 비밀번호 찾기 화면에서 발생하는 일회성 이벤트를 정의하는 Sealed Class
 */
sealed class FindPasswordEvent {
    /**
     * 이전 화면으로 돌아가기 이벤트
     */
    object NavigateBack : FindPasswordEvent()
    
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
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val verifyPasswordResetCodeUseCase: VerifyPasswordResetCodeUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val getAuthErrorMessageUseCase: GetAuthErrorMessageUseCase
) : ViewModel() {

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

    /**
     * 인증코드 입력값 변경 처리
     * @param code 변경된 인증코드 값
     */
    fun onAuthCodeChange(code: String) {
        _uiState.update { it.copy(authCode = code, errorMessage = null) }
    }

    /**
     * 새 비밀번호 입력값 변경 처리
     * @param password 변경된 비밀번호 값
     */
    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, errorMessage = null) }
    }

    /**
     * 새 비밀번호 확인 입력값 변경 처리
     * @param passwordConfirm 변경된 비밀번호 확인 값
     */
    fun onNewPasswordConfirmChange(passwordConfirm: String) {
        _uiState.update { it.copy(newPasswordConfirm = passwordConfirm, errorMessage = null) }
    }

    /**
     * 비밀번호 표시/숨김 상태 토글
     */
    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * 인증코드 요청 버튼 클릭 처리
     * 이메일 유효성 검사 후 인증코드 요청 UseCase 실행
     */
    fun onRequestAuthCodeClick() {
        val email = _uiState.value.email
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "올바른 이메일 형식이 아닙니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // RequestPasswordResetUseCase 호출
            val result = requestPasswordResetUseCase(email)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isEmailSent = true) }
                _eventFlow.emit(FindPasswordEvent.ShowSnackbar("인증코드가 이메일로 전송되었습니다."))
            }.onFailure { exception ->
                val errorMessage = getAuthErrorMessageUseCase.getPasswordResetErrorMessage(exception)
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }

    /**
     * 인증코드 확인 버튼 클릭 처리
     * 인증코드 유효성 검사 후 코드 확인 UseCase 실행
     */
    fun onConfirmAuthCodeClick() {
        val email = _uiState.value.email
        val code = _uiState.value.authCode
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "인증번호를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // VerifyPasswordResetCodeUseCase 호출
            val result = verifyPasswordResetCodeUseCase(email, code)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isEmailVerified = true) }
                _eventFlow.emit(FindPasswordEvent.ShowSnackbar("인증되었습니다. 새 비밀번호를 입력하세요."))
            }.onFailure { exception ->
                val errorMessage = getAuthErrorMessageUseCase.getPasswordResetErrorMessage(exception)
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }

    /**
     * 비밀번호 재설정 완료 버튼 클릭 처리
     * 비밀번호 유효성 검사 후 비밀번호 재설정 UseCase 실행
     */
    fun onCompletePasswordChangeClick() {
        val email = _uiState.value.email
        val code = _uiState.value.authCode
        val newPassword = _uiState.value.newPassword
        val confirmPassword = _uiState.value.newPasswordConfirm

        // 비밀번호 유효성 검사
        if (newPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "비밀번호는 최소 8자 이상이어야 합니다.") }
            return
        }
        // 영문자, 숫자, 특수문자 포함 여부 검사 로직 (SignUpViewModel과 유사한 로직으로 구현 가능)

        // 비밀번호 일치 확인
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "비밀번호가 일치하지 않습니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // ResetPasswordUseCase 호출
            val result = resetPasswordUseCase(email, code, newPassword)
            
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, passwordChangeSuccess = true) }
                _eventFlow.emit(FindPasswordEvent.ShowSnackbar("비밀번호가 성공적으로 변경되었습니다. 새 비밀번호로 로그인해주세요."))
                _eventFlow.emit(FindPasswordEvent.NavigateBack)
            }.onFailure { exception ->
                val errorMessage = getAuthErrorMessageUseCase.getPasswordResetErrorMessage(exception)
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }

    /**
     * 돌아가기 버튼 클릭 처리
     */
    fun onBackClick() {
        viewModelScope.launch {
            _eventFlow.emit(FindPasswordEvent.NavigateBack)
        }
    }

    fun errorMessageShown() {
        TODO("Not yet implemented")
    }
}