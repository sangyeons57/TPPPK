package com.example.feature_auth.viewmodel

import android.util.Patterns
import androidx.compose.ui.focus.FocusState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.SignUpFormFocusTarget
import com.example.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI 상태 정의 (이메일 인증 코드 관련 제거, 입력값 상태 추가)
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val name: String = "",
    val isPasswordVisible: Boolean = false, // 비밀번호 보이기/숨기기 상태 추가
    val isLoading: Boolean = false,
    val signUpSuccess: Boolean = false,

    // 필드별 에러 상태 추가
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmError: String? = null,
    val nameError: String? = null,

    //....포커스 된 경우 확인....
    val isEmailTouched: Boolean = false,
    val isPasswordTouched: Boolean = false,
    val isPasswordConfirmTouched: Boolean = false,
    val isNameTouched: Boolean = false
    )

// 이벤트 정의 (화면 이동 등)
sealed class SignUpEvent {
    object NavigateToLogin : SignUpEvent() // 예시: 성공 후 로그인 화면 이동
    data class ShowSnackbar(val message: String) : SignUpEvent()
    data class RequestFocus(val target: SignUpFormFocusTarget) : SignUpEvent()
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository // AuthRepository 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SignUpEvent>()
    val eventFlow = _eventFlow.asSharedFlow() // Composable에서 구독

    // 입력값 변경 시 해당 필드 에러 초기화
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email.trim(), emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, passwordConfirmError = null) } // 비밀번호 변경 시 확인 에러도 초기화
    }

    fun onPasswordConfirmChange(confirm: String) {
        _uiState.update { it.copy(passwordConfirm = confirm, passwordConfirmError = null) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name.trim(), nameError = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }


    // 회원가입 처리 함수
    fun signUp() {
        val state = _uiState.value

        // 유효성 검사 (순서 중요: 첫 번째 오류에서 멈추고 포커스 이동)
        var focusTarget: SignUpFormFocusTarget? = null

        if (checkEmail(state) && focusTarget == null)
            focusTarget = SignUpFormFocusTarget.EMAIL

        if (checkPassword(state) && focusTarget == null)
            focusTarget = SignUpFormFocusTarget.PASSWORD

        if (checkPasswordConfirm(state) && focusTarget == null)
            focusTarget = SignUpFormFocusTarget.PASSWORD_CONFIRM

        if (checkName(state) && focusTarget == null)
            focusTarget = SignUpFormFocusTarget.NAME


        // 유효성 검사 실패 시 포커스 요청 후 종료
        if (focusTarget != null) {
            viewModelScope.launch {
                focusTarget.let { _eventFlow.emit(SignUpEvent.RequestFocus(it)) }
            }
            return
        }
        // 유효성 검사 통과 시 회원가입 로직 진행
        viewModelScope.launch {
            // 로딩 시작 및 모든 필드 에러 초기화
            _uiState.update { it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                passwordConfirmError = null,
                nameError = null
            )}
            val result = authRepository.signUp(state.email, state.password, state.name)

            result.onSuccess { newUser ->
                _uiState.update { it.copy(isLoading = false, signUpSuccess = true) }
                _eventFlow.emit(SignUpEvent.ShowSnackbar("회원가입 성공! 로그인해주세요."))
                _eventFlow.emit(SignUpEvent.NavigateToLogin)
                println("회원가입 성공: ${newUser!!.email}")
            }.onFailure { exception ->
                val errorMessage = authRepository.getSignUpErrorMessage(exception)
                // 회원가입 API 실패 시 에러 처리 (여기서는 스낵바로 알림)
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(SignUpEvent.ShowSnackbar(errorMessage))
            }
        }
    }

    fun checkEmail(state: SignUpUiState): Boolean{
        if (state.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "올바른 이메일을 입력해주세요.") }
            return true
        }
        return false
    }
    fun onEmailFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isEmailTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isEmailTouched) {
            checkEmail(uiState.value)
        }
    }
    
    fun checkPassword(state: SignUpUiState): Boolean{
        val password = state.password

        // 길이 검사
        if (password.length < 8) {
            _uiState.update { it.copy(passwordError = "비밀번호는 최소 8자 이상이어야 합니다.") }
            return true
        }
        if (password.length > 100) {
            _uiState.update { it.copy(passwordError = "비밀번호는 최대 100자까지 가능합니다.") }
            return true
        }

        // 영문자 포함 여부
        if (!password.any { it.isLetter() }) {
            _uiState.update { it.copy(passwordError = "비밀번호에 최소 하나 이상의 영문자가 포함되어야 합니다.") }
            return true
        }

        // 숫자 포함 여부
        if (!password.any { it.isDigit() }) {
            _uiState.update { it.copy(passwordError = "비밀번호에 최소 하나 이상의 숫자가 포함되어야 합니다.") }
            return true
        }

        // 특수문자 포함 여부 (정의: 숫자/영문자 외의 모든 문자)
        if (!password.any { !it.isLetterOrDigit() }) {
            _uiState.update { it.copy(passwordError = "비밀번호에 최소 하나 이상의 특수문자가 포함되어야 합니다.") }
            return true
        }

        return false
    }
    fun onPasswordFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isPasswordTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isPasswordTouched) {
            checkPassword(uiState.value)
        }
    }

    fun checkPasswordConfirm(state: SignUpUiState): Boolean{
        if (state.password != state.passwordConfirm) {
            _uiState.update { it.copy(passwordConfirmError = "비밀번호가 일치하지 않습니다.") }
            return true
        }
        return false
    }
    fun onPasswordConfirmFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isPasswordConfirmTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isPasswordConfirmTouched) {
            checkPasswordConfirm(uiState.value)
        }
    }

    fun checkName(state: SignUpUiState): Boolean{
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "이름을 입력해주세요.") }
            return true
        }
        return false
    }
    fun onNameFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isNameTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isNameTouched) {
            checkName(uiState.value)
        }
    }
}