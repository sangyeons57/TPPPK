package com.example.feature_login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.constants.NavigationResultKeys
import com.example.core_navigation.core.FindPasswordRoute
import com.example.core_navigation.core.MainContainerRoute
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.ui.enum.LoginFormFocusTarget
import com.example.domain.model.vo.IsLoading
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.auth.AuthValidationUseCaseProvider
import com.example.domain.usecase.auth.session.WithdrawnAccountException
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
 * 로그인 화면의 UI 상태를 정의하는 데이터 클래스
 */
data class LoginUiState(
    val email: UserEmail = UserEmail.EMPTY,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val isLoading: IsLoading = IsLoading.False,
    val emailError: String? = null,
    val passwordError: String? = null,
)

/**
 * 로그인 화면에서 발생하는 일회성 이벤트를 정의하는 Sealed Class
 */
sealed class LoginEvent {
    /**
     * 스낵바 메시지 표시 이벤트
     * @param message 표시할 메시지
     */
    data class ShowSnackbar(val message: String) : LoginEvent()

    /**
     * 특정 입력 필드로 포커스 요청 이벤트
     * @param target 포커스 대상 필드
     */
    data class RequestFocus(val target: LoginFormFocusTarget) : LoginEvent()
}

/**
 * 로그인 화면의 비즈니스 로직을 처리하는 ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val authValidationUseCaseProvider: AuthValidationUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val authUseCases = authSessionUseCaseProvider.create()
    private val authValidationUseCases = authValidationUseCaseProvider.create()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // 회원가입 성공 후 이메일 인증 안내 메시지 확인
        checkSignUpSuccessResult()
    }

    /**
     * 이메일 입력값 변경 처리
     * 입력값 변경 시 해당 필드 에러 초기화 및 로그인 버튼 활성화 조건 업데이트
     * @param email 변경된 이메일 값
     */
    fun onEmailChange(email: UserEmail) {
        _uiState.update {
            it.copy(
                email = email.trim(),
                emailError = null,
                isLoginEnabled = email.trim().isNotBlank() && it.password.isNotBlank()
            )
        }
    }

    /**
     * 비밀번호 입력값 변경 처리
     * 입력값 변경 시 해당 필드 에러 초기화 및 로그인 버튼 활성화 조건 업데이트
     * @param password 변경된 비밀번호 값
     */
    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                isLoginEnabled = it.email.isNotBlank() && password.isNotBlank()
            )
        }
    }

    /**
     * 비밀번호 표시/숨김 상태 토글
     */
    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * 로그인 버튼 클릭 처리
     * 입력값 유효성 검사 후 로그인 UseCase 실행
     */
    fun onLoginClick() {
        val currentState = _uiState.value
        if (currentState.isLoading.value) return

        // 유효성 검사
        var isValid = true
        var focusTarget: LoginFormFocusTarget? = null

        if (currentState.email.isBlank() || !currentState.email.isEmailPatternAvailable()) {
            _uiState.update { it.copy(emailError = "올바른 이메일 형식이 아닙니다.") }
            focusTarget = LoginFormFocusTarget.EMAIL
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

        // 유효성 검사 통과 시 로그인 UseCase 실행
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = IsLoading.True,
                    emailError = null,
                    passwordError = null
                )
            }

            // LoginUseCase 호출
            val result = authUseCases.loginUseCase(currentState.email, currentState.password)

            when (result) {
                is CustomResult.Success -> {
                    // 로그인 성공 -> 메인 화면으로 이동 (백스택 클리어)
                    navigationManger.navigateToClearingBackStack(MainContainerRoute)
                }
                is CustomResult.Failure -> {
                    val exception = result.error
                    Log.e("LoginViewModel", "Login failed", exception)
                    val errorMessage = if (exception is WithdrawnAccountException) {
                        exception.message ?: "탈퇴한 계정입니다."
                    } else {
                        authValidationUseCases.getAuthErrorMessageUseCase(exception)
                    }
                    _uiState.update { it.copy(isLoading = IsLoading.False) }
                    _eventFlow.emit(LoginEvent.ShowSnackbar(errorMessage))
                }
                else -> {
                    _uiState.update { it.copy(isLoading = IsLoading.False) }
                    _eventFlow.emit(LoginEvent.ShowSnackbar("로그인 처리 중 오류가 발생했습니다."))
                }
            }
        }
    }

    /**
     * 비밀번호 찾기 버튼 클릭 처리
     */
    fun onFindPasswordClick() {
        navigationManger.navigateTo(FindPasswordRoute)
    }

    /**
     * 회원가입 버튼 클릭 처리
     */
    fun onSignUpClick() {
        navigationManger.navigateToSignUp()
    }

    /**
     * 회원가입 성공 후 이메일 인증 안내 메시지 확인
     */
    private fun checkSignUpSuccessResult() {
        val signUpSuccess = navigationManger.getResult<Boolean>(NavigationResultKeys.Auth.SIGNUP_SUCCESS_EMAIL_VERIFICATION)
        if (signUpSuccess == true) {
            viewModelScope.launch {
                _eventFlow.emit(LoginEvent.ShowSnackbar("회원가입이 완료되었습니다! 이메일 인증을 완료한 후 로그인해주세요."))
            }
        }
    }
}