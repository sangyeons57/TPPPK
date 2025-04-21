package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.LoginFormFocusTarget
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.AuthRepository
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.Calendar24HourUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI 상태 데이터 클래스
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false, // 비밀번호 보이기/숨기기 상태 추가
    val isLoginEnabled: Boolean = false, // 이메일/비밀번호 비어있지 않은지
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
)

// UI 이벤트 Sealed Class (네비게이션 등 일회성 이벤트)
sealed class LoginEvent {
    object NavigateToFindPassword : LoginEvent()
    object NavigateToSignUp : LoginEvent()
    data class LoginSuccess(val userId: String) : LoginEvent() // 로그인 성공 시 필요하면 (예: 메인 화면 이동)
    data class ShowSnackbar(val message: String) : LoginEvent()
    data class RequestFocus(val target: LoginFormFocusTarget) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository // Repository 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 입력값 변경 시 해당 필드 에러 초기화 및 로그인 버튼 활성화 조건 업데이트
    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                email = email.trim(),
                emailError = null,
                isLoginEnabled = email.trim().isNotBlank() && it.password.isNotBlank() // 실시간 활성화 조건 업데이트
            )
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                isLoginEnabled = it.email.isNotBlank() && password.isNotBlank() // 실시간 활성화 조건 업데이트
            )
        }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClick() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        // 유효성 검사
        var isValid = true
        var focusTarget: LoginFormFocusTarget? = null

        if (currentState.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update { it.copy(emailError = "올바른 이메일 형식이 아닙니다.") }
            if (focusTarget == null) focusTarget = LoginFormFocusTarget.EMAIL
            isValid = false
        }
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "비밀번호를 입력해주세요.") }
            if (focusTarget == null) focusTarget = LoginFormFocusTarget.PASSWORD
            isValid = false
        }

        // 유효성 검사 실패 시 포커스 이동 요청 후 종료
        if (!isValid) {
            viewModelScope.launch {
                focusTarget?.let { _eventFlow.emit(LoginEvent.RequestFocus(it)) }
            }
            return
        }

        // 유효성 검사 통과 시 Firebase 로그인 로직 진행
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailError = null, passwordError = null) }
            println("ViewModel: Login attempt with Email: ${currentState.email}")

            // *** Firebase 로그인 호출 ***
            val result = authRepository.login(currentState.email, currentState.password)

            result.onSuccess { loggedInUser ->
                // 로그인 성공
                _eventFlow.emit(LoginEvent.LoginSuccess(loggedInUser!!.userId))
                // 성공 시 isLoading은 false로 바꿀 필요 없음 (화면 전환)
            }.onFailure { exception ->
                // 로그인 실패
                val errorMessage = when (exception) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "존재하지 않는 이메일입니다."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "잘못된 비밀번호입니다."
                    // TODO: 네트워크 오류 등 다른 Firebase 예외 처리 추가
                    else -> exception.message ?: "로그인 실패"
                }
                _uiState.update { it.copy(isLoading = false) } // 로딩 종료
                _eventFlow.emit(LoginEvent.ShowSnackbar(errorMessage)) // 스낵바로 에러 알림
            }
        }
    }

    // 비밀번호 찾기 버튼 클릭
    fun onFindPasswordClick() {
        viewModelScope.launch {
            _eventFlow.emit(LoginEvent.NavigateToFindPassword)
        }
    }

    // 회원가입 버튼 클릭
    fun onSignUpClick() {
        viewModelScope.launch {
            _eventFlow.emit(LoginEvent.NavigateToSignUp)
        }
    }

    // errorMessageShown 관련 로직은 스낵바 처리 방식에 따라 조정 (필요 없을 수도 있음)
}