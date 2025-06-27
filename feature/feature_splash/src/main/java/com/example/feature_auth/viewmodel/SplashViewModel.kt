package com.example.feature_auth.viewmodel

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.core_navigation.core.MainContainerRoute
import com.example.core_navigation.core.LoginRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    // Provider를 통해 생성된 UseCase 그룹
    private val authUseCases = authSessionUseCaseProvider.create()

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
                val result = authUseCases.checkAuthenticationStatusUseCase()
                Log.d("SplashViewModel", "Auth check result: $result")

                result.onSuccess { isAuthenticatedAndVerified ->
                    if (isAuthenticatedAndVerified) {
                        Log.d("SplashViewModel", "User is authenticated and email verified - navigating to Home (clearing back stack)")
                        // Clear SplashScreen from the back stack to prevent returning to it via the back button
                        navigationManger.navigateToClearingBackStack(MainContainerRoute)
                    } else {
                        Log.d("SplashViewModel", "User is not authenticated or email not verified - navigating to Login (clearing back stack)")
                        navigationManger.navigateToClearingBackStack(LoginRoute)
                    }
                }.onFailure { exception ->
                    Log.e("SplashViewModel", "Auth check failed: ${exception.message}", exception)
                    
                    // Categorize errors for better handling
                    _uiState.update { it.copy(isLoading = false) }
                }

            } catch (e: Exception) {
                Log.e("SplashViewModel", "Unexpected error during auth check", e)
                _uiState.update { it.copy(isLoading = false) }
                navigationManger.navigateToClearingBackStack(LoginRoute) // Fallback to Login and clear Splash
            }
        }
    }

    /**
     * 인증 상태 확인을 재시도합니다. (무한 루프 방지를 위해 제한적으로 사용)
     */
    private var retryCount = 0
    private fun retryAuthCheck() {
        if (retryCount < 2) { // 최대 2회까지만 재시도
            retryCount++
            Log.d("SplashViewModel", "Retrying auth check (attempt $retryCount)")
            checkAuthenticationStatus()
        } else {
            Log.w("SplashViewModel", "Max retry attempts reached - navigating to Login")
            navigationManger.navigateToLogin()
        }
    }

    /**
     * 사용자가 버튼을 클릭하여 재시도할 때 호출합니다.
     */
    fun retry() {
        retryCount = 0 // Reset retry counter for manual retry
        _uiState.value = SplashUiState(true)
        checkAuthenticationStatus()
    }
} 