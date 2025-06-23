package com.example.feature_auth.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.auth.CheckAuthenticationStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 스플래시 화면의 UI 상태를 정의하는 데이터 클래스
 * 스플래시 화면은 로딩 상태만 필요함
 */
data class SplashUiState(val isLoading: Boolean = true)

/**
 * 스플래시 화면에서 발생하는 일회성 이벤트를 정의하는 Sealed Class
 */
sealed class SplashEvent {
    /**
     * 로그인 화면으로 이동 이벤트
     */
    object NavigateToLogin : SplashEvent() 
    
    /**
     * 메인 화면으로 이동 이벤트
     */
    object NavigateToMain : SplashEvent()
}

/**
 * 스플래시 화면의 비즈니스 로직을 처리하는 ViewModel
 * 세션 유효성을 확인하여 로그인 또는 메인 화면으로 이동 결정
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val checkAuthenticationStatusUseCase: CheckAuthenticationStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SplashEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        checkAuthenticationStatus()
    }

    /**
     * 현재 사용자의 인증 상태를 확인하고 적절한 화면으로 이동합니다.
     */
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            // 스플래시 화면을 최소 1초 이상 보여주기 위한 지연
            delay(1000)

            try {
                // Use UseCase to check status
                val result = checkAuthenticationStatusUseCase()
                Log.d("SplashViewModel", "Auth check result: $result")

                result.onSuccess { isSuccess ->
                    viewModelScope.launch {
                        if (isSuccess) {
                            _eventFlow.emit(SplashEvent.NavigateToMain)
                        } else {
                            _eventFlow.emit(SplashEvent.NavigateToLogin)
                        }
                    }
                }.onFailure { exception ->
                    viewModelScope.launch {
                        // Handle failure (e.g., network error during check)
                        _uiState.update { it.copy(isLoading = false) }
                        // Optionally show an error message or allow retry
                         _eventFlow.emit(SplashEvent.NavigateToLogin) // Or show error state
                        Log.e("SplashViewModel", "Auth check failed: $exception")
                         // Log error: Log.e("SplashViewModel", "Auth check failed", exception)
                    }
                }

            } catch (e: Exception) { // Catch potential unexpected errors
                _uiState.update { it.copy(isLoading = false) }
                 _eventFlow.emit(SplashEvent.NavigateToLogin) // Fallback to Login
                  Log.e("SplashViewModel", "Unexpected error", e)
            }
        }
    }

    /**
     * 사용자가 버튼을 클릭하여 재시도할 때 호출합니다.
     */
    fun retry() {
        _uiState.value = SplashUiState(true)
        checkAuthenticationStatus()
    }
} 