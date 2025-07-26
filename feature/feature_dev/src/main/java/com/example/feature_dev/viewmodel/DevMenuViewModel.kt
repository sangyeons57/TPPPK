package com.example.feature_dev.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.data.cache.ChatCacheManager
import com.example.domain.model.vo.DocumentId
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.dev.DevMenuUseCaseProvider
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import com.example.websocket.WebSocketConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val webSocketClient: ChatWebSocketClient,
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

    // 토큰 재발급을 위한 함수
    private suspend fun refreshAuthToken(): String? {
        addMessage("🔄 Refreshing token...")
        return when (val sessionResult = authUseCases.getCurrentUserSessionUseCase(forceRefresh = true)) {
            is CustomResult.Success -> {
                val newSession = sessionResult.data
                val newToken = newSession.idToken!!.value
                addMessage("🔄 New token issued successfully.")
                newToken
            }
            is CustomResult.Failure -> {
                addMessage("❌ Token refresh failed: ${sessionResult.error.message}")
                null
            }
            is CustomResult.Initial -> {
                addMessage("State: Initial")
                null
            }
            is CustomResult.Loading -> {
                addMessage("State: Loading...")
                null
            }
            is CustomResult.Progress -> {
                addMessage("State: Progress...")
                null
            }
        }
    }

    // WebSocket 테스트 관련 상태
    private val _webSocketConnectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Disconnected)
    val webSocketConnectionState: StateFlow<WebSocketConnectionState> = _webSocketConnectionState.asStateFlow()

    private val _isWebSocketConnecting = MutableStateFlow(false)
    val isWebSocketConnecting: StateFlow<Boolean> = _isWebSocketConnecting.asStateFlow()

    private val _webSocketMessages = MutableStateFlow<List<String>>(emptyList())
    val webSocketMessages: StateFlow<List<String>> = _webSocketMessages.asStateFlow()

    private val _lastSentCode = MutableStateFlow<String?>(null)
    val lastSentCode: StateFlow<String?> = _lastSentCode.asStateFlow()

    private val TEST_ROOM_ID = "test_websocket_room"
    private val SERVER_URL = "wss://websocket-chat-wizwlraydq-du.a.run.app/chat"

    // 로컬 채팅 캐시 삭제 진행 상태
    private val _isLocalChatCacheClearing = MutableStateFlow(false)
    val isLocalChatCacheClearing: StateFlow<Boolean> = _isLocalChatCacheClearing.asStateFlow()

    // 로컬 채팅 캐시 삭제 결과
    private val _localChatCacheClearResult = MutableStateFlow("")
    val localChatCacheClearResult: StateFlow<String> = _localChatCacheClearResult.asStateFlow()

    init {
        // 로그인 상태 확인
        checkLoginStatus()

        // WebSocket 연결 상태 관찰
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _webSocketConnectionState.value = state
                addMessage("Connection State: ${getConnectionStateText(state)}")
            }
        }

        // WebSocket 메시지 관찰
        viewModelScope.launch {
            webSocketClient.getChatMessages(TEST_ROOM_ID).collect { event ->
                when (event) {
                    is ChatWebSocketEvent.MessageReceived -> {
                        val message = "Received: ${event.content} (from: ${event.senderId})"
                        addMessage(message)

                        // 내가 보낸 코드가 돌아왔는지 확인
                        _lastSentCode.value?.let { sentCode ->
                            if (event.content.contains(sentCode)) {
                                addMessage("✅ SUCCESS: Round-trip confirmed! Code '$sentCode' received back")
                            }
                        }
                    }
                    is ChatWebSocketEvent.SystemMessage -> {
                        addMessage("System: ${event.content}")
                    }
                    is ChatWebSocketEvent.Error -> {
                        addMessage("❌ Error: ${event.message}")
                    }
                    else -> {
                        addMessage("Other event: $event")
                    }
                }
            }
        }
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

                    userSession.idToken?.let {
                        addMessage("✅ Logged in: ${userSession.email?.value}")
                    } ?: run {
                        addMessage("⚠️ Logged in, but token is missing: ${userSession.email?.value}")
                    }
                }
                is CustomResult.Failure -> {
                    _isLoggedIn.value = false
                    _currentUserInfo.value = null
                    addMessage("❌ Not logged in: ${sessionResult.error.message}")
                }
                is CustomResult.Initial -> addMessage("State: Initial")
                is CustomResult.Loading -> addMessage("State: Loading...")
                is CustomResult.Progress -> addMessage("State: Progress...")
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

    // WebSocket 테스트 함수들
    fun connectWebSocket() {
        if (!_isLoggedIn.value) {
            addMessage("❌ 로그인이 필요합니다. 먼저 로그인해주세요.")
            return
        }

        viewModelScope.launch {
            _isWebSocketConnecting.value = true
            addMessage("🔄 Connecting to WebSocket server...")

            try {
                // 현재 사용자 세션 정보 획득
                when (val sessionResult = authUseCases.getCurrentUserSessionUseCase()) {
                    is CustomResult.Success -> {
                        val userSession = sessionResult.data
                        val userId = userSession.userId
                        var token = userSession.idToken?.value

                        if (token == null) {
                            addMessage("❌ No valid token. Attempting to refresh...")
                            token = refreshAuthToken()
                            if (token == null) {
                                addMessage("❌ Token refresh failed. Aborting connection.")
                                _isWebSocketConnecting.value = false
                                return@launch
                            }
                        }

                        addMessage("🔄 Token validated. Attempting to connect WebSocket...")

                        val connectResult = webSocketClient.connect(SERVER_URL, token)

                        if (connectResult.isSuccess) {
                            addMessage("✅ Connected, waiting for authentication confirmation...")
                            
                            // 2단계: 인증 완료 대기 (서버가 handshake token으로 자동 인증)
                            val authResult = webSocketClient.waitForAuthentication(userId)
                            if (authResult.isSuccess) {
                                addMessage("✅ Authentication confirmed, joining room...")
                                
                                // 3단계: 방 입장
                                val joinResult = webSocketClient.joinRoom(TEST_ROOM_ID, userId)
                                if (joinResult.isSuccess) {
                                    addMessage("✅ Joined room: $TEST_ROOM_ID")
                                } else {
                                    addMessage("❌ Failed to join room: ${joinResult.exceptionOrNull()?.message}")
                                }
                            } else {
                                addMessage("❌ Authentication failed: ${authResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            val errorMessage = connectResult.exceptionOrNull()?.message ?: "Unknown error"
                            addMessage("❌ Connection failed: $errorMessage")

                            if (errorMessage.contains("1008") || errorMessage.contains("Authentication") || errorMessage.contains("Policy")) {
                                addMessage("🔄 Authentication error detected, retrying with a new token...")
                                val newToken = refreshAuthToken()
                                if (newToken != null) {
                                    val retryResult = webSocketClient.connect(SERVER_URL, newToken)
                                    if (retryResult.isSuccess) {
                                        addMessage("✅ Connected after token refresh, waiting for authentication...")
                                        
                                        // 재시도 시에도 인증 완료 대기 적용
                                        val retryAuthResult = webSocketClient.waitForAuthentication(userId)
                                        if (retryAuthResult.isSuccess) {
                                            addMessage("✅ Re-authentication confirmed, joining room...")
                                            val joinResult = webSocketClient.joinRoom(TEST_ROOM_ID, userId)
                                            if (joinResult.isSuccess) {
                                                addMessage("✅ Joined room: $TEST_ROOM_ID")
                                            } else {
                                                addMessage("❌ Failed to join room after retry: ${joinResult.exceptionOrNull()?.message}")
                                            }
                                        } else {
                                            addMessage("❌ Re-authentication failed: ${retryAuthResult.exceptionOrNull()?.message}")
                                        }
                                    } else {
                                        addMessage("❌ Connection failed even after token refresh: ${retryResult.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        }
                    }
                    is CustomResult.Failure -> {
                        addMessage("❌ Failed to get user information: ${sessionResult.error.message}")
                    }
                    is CustomResult.Initial -> addMessage("State: Initial")
                    is CustomResult.Loading -> addMessage("State: Loading...")
                    is CustomResult.Progress -> addMessage("State: Progress...")
                }
            } catch (e: Exception) {
                addMessage("❌ Exception during connection: ${e.message}")
            } finally {
                _isWebSocketConnecting.value = false
            }
        }
    }

    fun disconnectWebSocket() {
        viewModelScope.launch {
            addMessage("🔄 Disconnecting...")
            try {
                webSocketClient.leaveRoom(TEST_ROOM_ID)
                webSocketClient.disconnect()
                addMessage("✅ Disconnected")
            } catch (e: Exception) {
                addMessage("❌ Error during disconnect: ${e.message}")
            }
        }
    }

    fun sendHelloWorldTest() {
        if (!_isLoggedIn.value) {
            addMessage("❌ 로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            val randomCode = UUID.randomUUID().toString().take(8)
            val message = "Hello World $randomCode"
            _lastSentCode.value = randomCode

            addMessage("📤 Sending: $message")

            try {
                // 현재 로그인된 사용자 정보 획득
                when (val sessionResult = authUseCases.getCurrentUserSessionUseCase()) {
                    is CustomResult.Success -> {
                        val userId = sessionResult.data.userId
                        val result = webSocketClient.sendMessage(
                            roomId = TEST_ROOM_ID,
                            senderId = userId,
                            content = message,
                            messageId = DocumentId.generate()
                        )

                        if (result.isSuccess) {
                            addMessage("✅ Message sent successfully")
                        } else {
                            addMessage("❌ Failed to send message: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    is CustomResult.Failure -> {
                        addMessage("❌ Failed to get user information: ${sessionResult.error.message}")
                    }

                    is CustomResult.Initial -> addMessage("State: Initial")
                    is CustomResult.Loading -> addMessage("State: Loading...")
                    is CustomResult.Progress -> addMessage("State: Progress...")
                }
            } catch (e: Exception) {
                addMessage("❌ Exception during send: ${e.message}")
            }
        }
    }

    fun clearWebSocketMessages() {
        _webSocketMessages.value = emptyList()
        _lastSentCode.value = null
    }

    private fun addMessage(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val formattedMessage = "[$timestamp] $message"

        _webSocketMessages.value = (_webSocketMessages.value + formattedMessage).takeLast(50)
    }

    private fun getConnectionStateText(state: WebSocketConnectionState): String {
        return when (state) {
            is WebSocketConnectionState.Connected -> "Connected"
            is WebSocketConnectionState.Connecting -> "Connecting..."
            is WebSocketConnectionState.Disconnected -> "Disconnected"
            is WebSocketConnectionState.Error -> "Error: ${state.message}"
        }
    }

    fun getWebSocketStatusText(): String {
        return getConnectionStateText(_webSocketConnectionState.value)
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
