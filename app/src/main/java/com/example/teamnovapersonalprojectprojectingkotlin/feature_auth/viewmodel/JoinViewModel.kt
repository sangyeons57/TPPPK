package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.ui.JoinDestinations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- 상태 클래스 정의 ---

data class InputState(
    val value: String = "",
    val isValid: Boolean = true, // 즉각적인 형식/길이 등 유효성
    val errorMessage: String? = null
)

data class SimpleJoinUiState( // UI 상태 홀더
    val emailState: InputState = InputState(),
    val authCodeState: InputState = InputState(),
    val nameState: InputState = InputState(),
    val passwordState: InputState = InputState(),
    val confirmPasswordState: InputState = InputState(),
    val passwordsMatch: Boolean = true
)

// --- 이벤트 클래스 정의 ---
sealed class JoinEvent {
    data class NavigateTo(val destination: String) : JoinEvent()
    object NavigateBack : JoinEvent()
    object RegistrationSuccess : JoinEvent()
    data class ShowSnackbar(val message: String) : JoinEvent()
}

// --- ViewModel ---

class JoinViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SimpleJoinUiState())
    val uiState: StateFlow<SimpleJoinUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JoinEvent>()
    val eventFlow = _eventFlow.asSharedFlow() // UI 이벤트 전달용

    // --- 입력값 변경 처리 함수 ---

    fun onEmailChange(email: String) {
        val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()
        _uiState.update {
            it.copy(
                emailState = it.emailState.copy(
                    value = email,
                    isValid = isValid,
                    errorMessage = if (!isValid && email.isNotEmpty()) "유효한 이메일 형식이 아닙니다." else null
                )
            )
        }
    }

    fun onAuthCodeChange(code: String) {
        val isValid = code.length <= 6 && code.all { it.isDigit() }
        _uiState.update {
            it.copy(
                authCodeState = it.authCodeState.copy(
                    value = code,
                    isValid = isValid || code.isEmpty(),
                    errorMessage = if (!isValid && code.isNotEmpty()) "6자리 숫자를 입력하세요." else null
                )
            )
        }
    }

    fun onNameChange(name: String) {
        val isValid = name.isNotBlank()
        _uiState.update {
            it.copy(
                nameState = it.nameState.copy(
                    value = name,
                    isValid = isValid || name.isEmpty(),
                    errorMessage = if (!isValid && name.isNotEmpty()) "이름을 입력하세요." else null
                )
            )
        }
    }

    fun onPasswordChange(password: String) {
        val isValid = password.length >= 8 || password.isEmpty()
        val confirmPassword = _uiState.value.confirmPasswordState.value
        _uiState.update {
            it.copy(
                passwordState = it.passwordState.copy(
                    value = password,
                    isValid = isValid,
                    errorMessage = if (!isValid && password.isNotEmpty()) "8자 이상 입력하세요." else null
                ),
                passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()
            )
        }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        val password = _uiState.value.passwordState.value
        val passwordsMatch = password == confirmPassword
        _uiState.update {
            it.copy(
                confirmPasswordState = it.confirmPasswordState.copy(
                    value = confirmPassword,
                    isValid = passwordsMatch || confirmPassword.isEmpty()
                ),
                passwordsMatch = passwordsMatch
            )
        }
    }

    // --- 액션 처리 함수 (단순화: 기본 유효성 검사 후 네비게이션 이벤트 발생) ---

    fun sendVerificationCode() {
        val emailState = _uiState.value.emailState
        if (!emailState.isValid || emailState.value.isEmpty()) {
            viewModelScope.launch { _eventFlow.emit(JoinEvent.ShowSnackbar("이메일을 올바르게 입력해주세요.")) }
            return
        }
        viewModelScope.launch {
            // 서버 로직 없이 바로 다음 단계 이동 이벤트 발생
            _eventFlow.emit(JoinEvent.NavigateTo(JoinDestinations.authCodeRoute(emailState.value)))
        }
    }

    fun verifyAuthCode() {
        val email = _uiState.value.emailState.value
        val authCodeState = _uiState.value.authCodeState
        if (!authCodeState.isValid || authCodeState.value.length != 6) {
            viewModelScope.launch { _eventFlow.emit(JoinEvent.ShowSnackbar("인증번호 6자리를 입력해주세요.")) }
            return
        }
        viewModelScope.launch {
            // 서버 로직 없이 바로 다음 단계 이동 이벤트 발생
            _eventFlow.emit(JoinEvent.NavigateTo(JoinDestinations.setNameRoute(email)))
        }
    }

    fun submitName() {
        val email = _uiState.value.emailState.value
        val nameState = _uiState.value.nameState
        if (!nameState.isValid || nameState.value.isEmpty()) {
            viewModelScope.launch { _eventFlow.emit(JoinEvent.ShowSnackbar("이름을 입력해주세요.")) }
            return
        }
        viewModelScope.launch {
            // 서버 로직 없이 바로 다음 단계 이동 이벤트 발생
            _eventFlow.emit(JoinEvent.NavigateTo(JoinDestinations.setPasswordRoute(email, nameState.value)))
        }
    }

    fun submitPasswordAndRegister() {
        val passwordState = _uiState.value.passwordState
        val confirmPasswordState = _uiState.value.confirmPasswordState
        val passwordsMatch = _uiState.value.passwordsMatch

        if (!passwordState.isValid || passwordState.value.isEmpty()) {
            viewModelScope.launch { _eventFlow.emit(JoinEvent.ShowSnackbar("비밀번호를 8자 이상 입력해주세요.")) }
            return
        }
        if (!passwordsMatch || confirmPasswordState.value.isEmpty()) {
            viewModelScope.launch { _eventFlow.emit(JoinEvent.ShowSnackbar("비밀번호가 일치하지 않습니다.")) }
            return
        }
        viewModelScope.launch {
            // 서버 로직 없이 바로 최종 성공 이벤트 발생
            _eventFlow.emit(JoinEvent.RegistrationSuccess)
        }
    }

    // 뒤로가기 이벤트 발행
    fun navigateBack() {
        viewModelScope.launch {
            _eventFlow.emit(JoinEvent.NavigateBack)
        }
    }
}