package com.example.feature_auth.viewmodel

import android.util.Patterns
import androidx.compose.ui.focus.FocusState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.SignUpFormFocusTarget
import com.example.domain.usecase.auth.GetAuthErrorMessageUseCase
import com.example.domain.usecase.auth.SignUpUseCase
import com.example.domain.usecase.user.CheckNicknameAvailabilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant

/**
 * 회원가입 화면의 UI 상태를 정의하는 데이터 클래스
 */
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val name: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val signUpSuccess: Boolean = false,
    val isNotUnder14: Boolean = false, // 만 14세 미만 여부
    val agreeWithTerms: Boolean = false, // 이용약관 및 개인정보처리방침 동의 여부

    // Email Verification States
    val isEmailVerificationSent: Boolean = false, // 이메일 인증 메일 발송 여부
    val isEmailVerified: Boolean = false, // 이메일 인증 완료 여부
    val emailVerificationError: String? = null, // 이메일 인증 관련 에러 메시지
    val isVerifyingEmail: Boolean = false, // 이메일 인증 시도 중 (로딩) 상태

    // 필드별 에러 상태
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmError: String? = null,
    val nameError: String? = null,

    // 필드별 포커스 상태
    val isEmailTouched: Boolean = false,
    val isPasswordTouched: Boolean = false,
    val isPasswordConfirmTouched: Boolean = false,
    val isNameTouched: Boolean = false
)

/**
 * 회원가입 화면에서 발생하는 일회성 이벤트를 정의하는 Sealed Class
 */
sealed class SignUpEvent {
    /**
     * 로그인 화면으로 이동 이벤트
     */
    object NavigateToLogin : SignUpEvent()

    /**
     * 서비스 이용약관 화면으로 이동 이벤트
     */
    object NavigateToTermsOfService : SignUpEvent()

    /**
     * 개인정보 처리방침 화면으로 이동 이벤트
     */
    object NavigateToPrivacyPolicy : SignUpEvent()

    /**
     * 스낵바 메시지 표시 이벤트
     * @param message 표시할 메시지
     */
    data class ShowSnackbar(val message: String) : SignUpEvent()

    /**
     * 특정 입력 필드로 포커스 요청 이벤트
     * @param target 포커스 대상 필드
     */
    data class RequestFocus(val target: SignUpFormFocusTarget) : SignUpEvent()
}

/**
 * 회원가입 화면의 비즈니스 로직을 처리하는 ViewModel
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val checkNicknameAvailabilityUseCase: CheckNicknameAvailabilityUseCase,
    private val getAuthErrorMessageUseCase: GetAuthErrorMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SignUpEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 이메일 입력값 변경 처리
     * @param email 변경된 이메일 값
     */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email.trim(), emailError = null) }
    }

    /**
     * 비밀번호 입력값 변경 처리
     * 비밀번호 변경 시 확인 에러도 초기화
     * @param password 변경된 비밀번호 값
     */
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, passwordConfirmError = null) }
    }

    /**
     * 비밀번호 확인 입력값 변경 처리
     * @param confirm 변경된 비밀번호 확인 값
     */
    fun onPasswordConfirmChange(confirm: String) {
        _uiState.update { it.copy(passwordConfirm = confirm, passwordConfirmError = null) }
    }

    /**
     * 이름(닉네임) 입력값 변경 처리
     * @param name 변경된 이름 값
     */
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name.trim(), nameError = null) }
    }

    /**
     * 비밀번호 표시/숨김 상태 토글
     */
    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * 만 14세 미만 체크박스 상태 변경 처리
     * @param isChecked 변경된 체크 상태
     */
    fun onUnder14CheckedChange(isChecked: Boolean) {
        _uiState.update { it.copy(isNotUnder14 = isChecked) }
    }

    /**
     * 이용약관 및 개인정보처리방침 동의 체크박스 상태 변경 처리
     * @param isChecked 변경된 체크 상태
     */
    fun onAgreeWithTermsChange(isChecked: Boolean) {
        _uiState.update { it.copy(agreeWithTerms = isChecked) }
    }

    /**
     * 서비스 이용약관 클릭 처리
     */
    fun onTermsOfServiceClick() {
        viewModelScope.launch {
            _eventFlow.emit(SignUpEvent.NavigateToTermsOfService)
        }
    }

    /**
     * 개인정보 처리방침 클릭 처리
     */
    fun onPrivacyPolicyClick() {
        viewModelScope.launch {
            _eventFlow.emit(SignUpEvent.NavigateToPrivacyPolicy)
        }
    }

    /**
     * 회원가입 처리 함수
     * 각 필드 유효성 검사 후 회원가입 UseCase 실행
     */
    fun signUp() {
        val state = _uiState.value

        // 필수 조건 검사 (나이, 약관)
        if (!state.isNotUnder14) {
            viewModelScope.launch { _eventFlow.emit(SignUpEvent.ShowSnackbar("만 14세 미만은 가입할 수 없습니다.")) }
            return
        }
        if (!state.agreeWithTerms) {
            viewModelScope.launch { _eventFlow.emit(SignUpEvent.ShowSnackbar("이용약관 및 개인정보처리방침에 동의해주세요.")) }
            return
        }

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

            // 닉네임 중복 확인
            val nicknameCheck = checkNicknameAvailabilityUseCase(state.name)
            if (nicknameCheck.isFailure || nicknameCheck.getOrNull() == false) {
                _uiState.update { it.copy(
                    isLoading = false,
                    nameError = "이미 사용 중인 닉네임입니다."
                )}
                _eventFlow.emit(SignUpEvent.RequestFocus(SignUpFormFocusTarget.NAME))
                return@launch
            }

            // 정책 동의 시간 기록
            val consentTimeStamp = Instant.now()

            // SignUpUseCase 호출
            val result = signUpUseCase(state.email, state.password, state.name, consentTimeStamp)

            result.onSuccess { newUser ->
                _uiState.update { it.copy(isLoading = false, signUpSuccess = true) }
                _eventFlow.emit(SignUpEvent.ShowSnackbar("회원가입 성공! 로그인해주세요."))
                _eventFlow.emit(SignUpEvent.NavigateToLogin)
                println("회원가입 성공: ${newUser!!.email}")
            }.onFailure { exception ->
                val errorMessage = getAuthErrorMessageUseCase.getSignUpErrorMessage(exception)
                // 회원가입 API 실패 시 에러 처리
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(SignUpEvent.ShowSnackbar(errorMessage))
            }
        }
    }

    /**
     * 이메일 유효성 검사
     * @param state 현재 UI 상태
     * @return 유효성 검사 실패 여부 (true: 실패, false: 성공)
     */
    fun checkEmail(state: SignUpUiState): Boolean {
        if (state.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "올바른 이메일을 입력해주세요.") }
            return true
        }
        return false
    }

    /**
     * 이메일 포커스 상태 변경 처리
     * @param focusState 포커스 상태
     */
    fun onEmailFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isEmailTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isEmailTouched) {
            checkEmail(uiState.value)
        }
    }

    /**
     * 비밀번호 포커스 상태 변경 처리   
     * @param focusState 포커스 상태
     */
    fun onPasswordFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isPasswordTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isPasswordTouched) {
            checkPassword(uiState.value)
        }
    }

    /**
     * 비밀번호 유효성 검사
     * @param state 현재 UI 상태
     * @return 유효성 검사 실패 여부 (true: 실패, false: 성공)
     */
    fun checkPassword(state: SignUpUiState): Boolean {
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

    /**
     * 비밀번호 확인 유효성 검사
     * @param state 현재 UI 상태
     * @return 유효성 검사 실패 여부 (true: 실패, false: 성공)
     */
    fun checkPasswordConfirm(state: SignUpUiState): Boolean {
        if (state.password != state.passwordConfirm) {
            _uiState.update { it.copy(passwordConfirmError = "비밀번호가 일치하지 않습니다.") }
            return true
        }
        return false
    }

    /**
     * 비밀번호 확인 포커스 상태 변경 처리
     * @param focusState 포커스 상태
     */
    fun onPasswordConfirmFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isPasswordConfirmTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isPasswordConfirmTouched) {
            checkPasswordConfirm(uiState.value)
        }
    }

    /**
     * 이름(닉네임) 유효성 검사
     * @param state 현재 UI 상태
     * @return 유효성 검사 실패 여부 (true: 실패, false: 성공)
     */
    fun checkName(state: SignUpUiState): Boolean {
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "이름을 입력해주세요.") }
            return true
        }
        // TODO: 이름(닉네임) 정책에 따른 추가 검사 (길이, 특수문자 등)
        if (state.name.length < 2) {
            _uiState.update { it.copy(nameError = "이름은 최소 2자 이상이어야 합니다.") }
            return true
        }
        if (state.name.length > 20) {
            _uiState.update { it.copy(nameError = "이름은 최대 20자까지 가능합니다.") }
            return true
        }
        return false
    }

    /**
     * 이름(닉네임) 포커스 상태 변경 처리
     * @param focusState 포커스 상태
     */
    fun onNameFocus(focusState: FocusState) {
        if (focusState.isFocused) {
            _uiState.update { it.copy(isNameTouched = true) }
        } else if (!focusState.isFocused && uiState.value.isNameTouched) {
            checkName(uiState.value)
        }
    }
}