package com.example.feature_dev.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.data_core.cache.ChatCacheManager
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.dev.DevMenuUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val devMenuUseCaseProvider: DevMenuUseCaseProvider,
    private val chatCacheManager: ChatCacheManager // 추가
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Firestore 캐시 삭제 결과 메시지
    private val _cacheClearResult = MutableStateFlow("")
    val cacheClearResult: StateFlow<String> = _cacheClearResult.asStateFlow()

    // 캐시 삭제 진행 상태
    private val _isCacheClearing = MutableStateFlow(false)
    val isCacheClearing: StateFlow<Boolean> = _isCacheClearing.asStateFlow()

    // 로그인 상태 관련
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserInfo = MutableStateFlow<String?>(null)
    val currentUserInfo: StateFlow<String?> = _currentUserInfo.asStateFlow()

    private val authUseCases by lazy { authSessionUseCaseProvider.create() }


    // 로컬 채팅 캐시 삭제 진행 상태
    private val _isLocalChatCacheClearing = MutableStateFlow(false)
    val isLocalChatCacheClearing: StateFlow<Boolean> = _isLocalChatCacheClearing.asStateFlow()

    // 로컬 채팅 캐시 삭제 결과
    private val _localChatCacheClearResult = MutableStateFlow("")
    val localChatCacheClearResult: StateFlow<String> = _localChatCacheClearResult.asStateFlow()

    init {
        // 로그인 상태 확인
        checkLoginStatus()
    }

    /**
     * 결과를 초기화합니다.
     */
    fun clearResult() {
        _cacheClearResult.value = ""
    }

    /**
     * 현재 로그인 상태를 확인합니다.
     */
    private fun checkLoginStatus() {
        viewModelScope.launch {
            when (val sessionResult = authUseCases.getCurrentUserSessionUseCase()) {
                is CustomResult.Success -> {
                    val userSession = sessionResult.data
                    _isLoggedIn.value = true
                    _currentUserInfo.value = "${userSession.displayName?.value ?: "사용자"} (${userSession.email?.value})"
                }
                is CustomResult.Failure -> {
                    _isLoggedIn.value = false
                    _currentUserInfo.value = null
                }
                else -> {
                    // Loading, Initial, Progress states
                }
            }
        }
    }

    /**
     * 로그인 상태를 새로고침합니다.
     */
    fun refreshLoginStatus() {
        checkLoginStatus()
    }

    /**
     * Firestore 캐시 정보를 표시합니다.
     * 실제 캐시 삭제는 Firestore의 네이티브 캐싱에 의해 관리됩니다.
     */
    fun clearFirestoreCache() {
        viewModelScope.launch {
            _isCacheClearing.value = true
            _cacheClearResult.value = "캐시 정보 확인 중..."

            // Firestore는 네이티브 캐싱을 사용하므로 수동 캐시 삭제 불필요
            Log.d("DevMenuViewModel", "Firestore uses native caching - manual cache clearing not required")
            _cacheClearResult.value = "정보: Firestore는 네이티브 캐싱을 사용합니다. 수동 캐시 삭제가 필요하지 않습니다."

            _isCacheClearing.value = false
        }
    }

    /**
     * 로컬 채팅 캐시 전체 삭제
     */
    fun clearAllLocalChatCache() {
        viewModelScope.launch {
            _isLocalChatCacheClearing.value = true
            _localChatCacheClearResult.value = "로컬 채팅 캐시 삭제 중..."
            try {
                chatCacheManager.clearAllCache()
                _localChatCacheClearResult.value = "성공: 모든 채팅 캐시가 삭제되었습니다."
            } catch (e: Exception) {
                _localChatCacheClearResult.value = "실패: ${e.message ?: "알 수 없는 오류"}"
            } finally {
                _isLocalChatCacheClearing.value = false
            }
        }
    }


    /**
     * FCM 테스트용 Functions 호출 (UseCaseProvider 경유)
     */
    fun sendFcmTestNotification(channelId: String = "test_channel_id") {
        if (!_isLoggedIn.value) {
            Log.d("DevMenuViewModel-FCM", "❌ 로그인이 필요합니다.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val sessionResult = authUseCases.getCurrentUserSessionUseCase()) {
                    is CustomResult.Success -> {
                        val userId = sessionResult.data.userId.value
                        val useCases = devMenuUseCaseProvider.create()
                        val result = useCases.sendFcmTestNotificationUseCase(userId, channelId)
                        when (result) {
                            is CustomResult.Success -> Log.d(
                                "DevMenuViewModel-FCM",
                                "✅ FCM 테스트 알림 전송 성공: ${result.data}"
                            )

                            is CustomResult.Failure -> Log.d(
                                "DevMenuViewModel-FCM",
                                "❌ FCM 테스트 알림 실패: ${result.error.message}"
                            )

                            else -> Log.d("DevMenuViewModel-FCM", "⚠️ FCM 테스트 알림 결과: $result")
                        }
                    }

                    is CustomResult.Failure -> Log.d(
                        "DevMenuViewModel-FCM",
                        "❌ 유저 정보 조회 실패: ${sessionResult.error.message}"
                    )

                    else -> Log.d("DevMenuViewModel-FCM", "⚠️ 유저 정보 조회 결과: $sessionResult")
                }
            } catch (e: Exception) {
                Log.d("DevMenuViewModel-FCM", "❌ FCM 테스트 알림 예외: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
