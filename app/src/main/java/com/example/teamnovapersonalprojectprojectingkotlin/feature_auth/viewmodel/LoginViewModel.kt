package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI 상태를 나타내는 데이터 클래스
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoginEnabled: Boolean = false, // 이메일/비밀번호가 비어있지 않은지 여부 (Derived State)
    val isLoading: Boolean = false,     // 로그인 시도 중 로딩 상태 (향후 사용)
    val errorMessage: String? = null    // 로그인 에러 메시지 (향후 사용)
)

// ViewModel에서 발생시키는 UI 이벤트 (네비게이션, 스낵바 등)
sealed class LoginEvent {
    object NavigateToFindPassword : LoginEvent()
    object NavigateToSignUp : LoginEvent()
    // object LoginSuccess : LoginEvent() // 로그인 성공 시 이벤트 (필요 시)
    data class ShowSnackbar(val message: String) : LoginEvent()
}

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // UI 이벤트를 전달하기 위한 SharedFlow
    private val _eventFlow = MutableSharedFlow<LoginEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 이메일 입력 변경 처리
    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                isLoginEnabled = email.isNotBlank() && it.password.isNotBlank() // 로그인 버튼 활성화 상태 업데이트
            )
        }
    }

    // 비밀번호 입력 변경 처리
    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                isLoginEnabled = it.email.isNotBlank() && password.isNotBlank() // 로그인 버튼 활성화 상태 업데이트
            )
        }
    }

    // 비밀번호 보이기/숨기기 토글 처리
    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    // 로그인 버튼 클릭 처리
    fun onLoginClick() {
        val currentState = _uiState.value
        if (!currentState.isLoginEnabled) return // 버튼이 비활성화 상태면 무시

        viewModelScope.launch {
            // TODO: 실제 로그인 로직 구현 (Model/Repository 호출)
            // _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // try {
            //    val result = repository.login(currentState.email, currentState.password)
            //    if (result.isSuccess) {
            //        _eventFlow.emit(LoginEvent.LoginSuccess)
            //    } else {
            //        _uiState.update { it.copy(errorMessage = "로그인 실패") }
            //        _eventFlow.emit(LoginEvent.ShowSnackbar("로그인 정보를 확인해주세요."))
            //    }
            // } catch (e: Exception) {
            //     _uiState.update { it.copy(errorMessage = "오류 발생: ${e.message}") }
            //    _eventFlow.emit(LoginEvent.ShowSnackbar("로그인 중 오류가 발생했습니다."))
            // } finally {
            //    _uiState.update { it.copy(isLoading = false) }
            // }

            // --- 현재는 UI 작동 확인을 위해 간단히 스낵바 이벤트만 발생 ---
            println("ViewModel: Login attempt with Email: ${currentState.email}, Password: ${currentState.password}")
            _eventFlow.emit(LoginEvent.ShowSnackbar("로그인 시도: ${currentState.email}"))
            // ----------------------------------------------------------
        }
    }

    // 비밀번호 찾기 버튼 클릭 처리
    fun onFindPasswordClick() {
        viewModelScope.launch {
            _eventFlow.emit(LoginEvent.NavigateToFindPassword)
        }
    }

    // 회원가입 버튼 클릭 처리
    fun onSignUpClick() {
        viewModelScope.launch {
            _eventFlow.emit(LoginEvent.NavigateToSignUp)
        }
    }
}