package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 (Splash는 상태가 거의 없을 수 있음) ---
// data class SplashUiState(val isLoading: Boolean = true)

// --- 이벤트 ---
sealed class SplashEvent {
    object NavigateToLogin : SplashEvent() // 로그인 화면으로 이동
    object NavigateToMain : SplashEvent()  // 메인 화면으로 이동
}


@HiltViewModel
class SplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 사용하지 않더라도 Hilt 주입 위해 필요할 수 있음
    private val authRepository: AuthRepository,
    // TODO: private val authRepository: AuthRepository // 로그인 상태 확인 등
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<SplashEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        checkNextDestination()
    }

    /**
     * 초기 상태 확인 후 다음 목적지 결정
     */
    private fun checkNextDestination() {
        viewModelScope.launch {
            // 최소 스플래시 표시 시간 (예: 1.5초)
            delay(1500L)

            // --- ↓↓↓ 주입된 Repository 사용 ↓↓↓ ---
            val isLoggedIn = try {
                authRepository.isLoggedIn()
            } catch (e: Exception) {
                // 네트워크 오류 등 예외 발생 시 로그인 안 된 것으로 간주 (또는 에러 처리)
                println("Error checking login status: ${e.message}")
                false
            }
            // --- ↑↑↑ 주입된 Repository 사용 ↑↑↑ ---


            if (isLoggedIn) {
                _eventFlow.emit(SplashEvent.NavigateToMain)
            } else {
                _eventFlow.emit(SplashEvent.NavigateToLogin)
            }
        }
    }
}