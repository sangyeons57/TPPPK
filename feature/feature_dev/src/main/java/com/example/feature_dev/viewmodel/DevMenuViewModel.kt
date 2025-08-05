package com.example.feature_dev.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.data_repository.util.RoomDatabaseLogger
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.vo.DocumentId
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.dev.DevMenuUseCaseProvider
import com.example.domain_usecase.usecase.sync.ResetAndSyncUseCase
import com.example.domain_usecase.usecase.sync.SyncUseCase
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.event.WebSocketDomainEvent
import com.example.websocket.usecase.WebSocketUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DevMenuViewModel @Inject constructor(
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val devMenuUseCaseProvider: DevMenuUseCaseProvider,
    private val syncManager: SyncCoordinator,
    private val syncUseCase: SyncUseCase,
    private val resetAndSyncUseCase: ResetAndSyncUseCase,
    private val roomDatabaseLogger: RoomDatabaseLogger
) : ViewModel() {

    // WebSocket use cases for dev testing
    private val webSocketUseCases by lazy {
        webSocketUseCaseProvider.createForRoom(TEST_ROOM_ID)
    }

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

    // 동기화 관련 상태
    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Room DB 검사 관련 상태
    private val _isDbInspecting = MutableStateFlow(false)
    val isDbInspecting: StateFlow<Boolean> = _isDbInspecting.asStateFlow()

    private val _dbInspectionResult = MutableStateFlow("")
    val dbInspectionResult: StateFlow<String> = _dbInspectionResult.asStateFlow()

    private val _selectedChannelId = MutableStateFlow("")
    val selectedChannelId: StateFlow<String> = _selectedChannelId.asStateFlow()

    init {
        // 로그인 상태 확인
        checkLoginStatus()

        // WebSocket 연결 상태 관찰
        viewModelScope.launch {
            webSocketUseCaseProvider.create().getConnectionStateUseCase().collect { state ->
                _webSocketConnectionState.value = state
                addMessage("Connection State: ${getConnectionStateText(state)}")
            }
        }

        // WebSocket 메시지 관찰
        viewModelScope.launch {
            webSocketUseCases.subscribeToRoomEventsUseCase().collect { event ->
                when (event) {
                    is WebSocketDomainEvent.MessageReceived -> {
                        val receivedMessage = "Received: ${event.content} (from: ${event.senderId})"
                        addMessage(receivedMessage)

                        // 내가 보낸 코드가 돌아왔는지 확인
                        _lastSentCode.value?.let { sentCode ->
                            if (event.content.contains(sentCode)) {
                                addMessage("✅ SUCCESS: Round-trip confirmed! Code '$sentCode' received back")
                            }
                        }
                    }

                    is WebSocketDomainEvent.SystemMessage -> {
                        addMessage("System: ${event.content}")
                    }

                    is WebSocketDomainEvent.Error -> {
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
                        userSession.userId
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

                        try {
                            val connectResult =
                                webSocketUseCaseProvider.create().connectUseCase(SERVER_URL, token)
                            if (connectResult.isSuccess) {
                                addMessage("✅ Connected and authenticated, joining room...")

                                // 방 입장 (connect가 성공하면 인증도 완료됨)
                                val joinResult =
                                    webSocketUseCases.joinRoomUseCase(userSession.userId)
                                if (joinResult.isSuccess) {
                                    addMessage("✅ Joined room: $TEST_ROOM_ID")
                                } else {
                                    addMessage("❌ Failed to join room: ${joinResult.exceptionOrNull()?.message}")
                                }
                            } else {
                                val errorMessage =
                                    connectResult.exceptionOrNull()?.message ?: "Unknown error"
                                addMessage("❌ Connection failed: $errorMessage")

                                if (errorMessage.contains("1008") || errorMessage.contains("Authentication") || errorMessage.contains(
                                        "Policy"
                                    )
                                ) {
                                    addMessage("🔄 Authentication error detected, retrying with a new token...")
                                    val newToken = refreshAuthToken()
                                    if (newToken != null) {
                                        val retryResult = webSocketUseCaseProvider.create()
                                            .connectUseCase(SERVER_URL, newToken)
                                        if (retryResult.isSuccess) {
                                            addMessage("✅ Connected after token refresh, joining room...")
                                            val joinResult =
                                                webSocketUseCases.joinRoomUseCase(userSession.userId)
                                            if (joinResult.isSuccess) {
                                                addMessage("✅ Joined room after retry: $TEST_ROOM_ID")
                                            } else {
                                                addMessage("❌ Failed to join room after retry: ${joinResult.exceptionOrNull()?.message}")
                                            }
                                        } else {
                                            addMessage("❌ Connection failed even after token refresh: ${retryResult.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            addMessage("❌ Exception during WebSocket connection: ${e.message}")
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
                // 방 나가기
                val leaveResult = webSocketUseCases.leaveRoomUseCase()
                if (leaveResult.isSuccess) {
                    addMessage("✅ Left room: $TEST_ROOM_ID")
                } else {
                    addMessage("⚠️ Failed to leave room: ${leaveResult.exceptionOrNull()?.message}")
                }

                // 연결 해제
                webSocketUseCaseProvider.create().disconnectUseCase()
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
                        val result = webSocketUseCases.sendMessageUseCase(
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
            is WebSocketConnectionState.Authenticating -> "Authenticating..."
            is WebSocketConnectionState.Disconnected -> "Disconnected"
            is WebSocketConnectionState.Reconnecting -> "Reconnecting..."
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

    // ================================
    // 동기화 기능
    // ================================

    /**
     * 증분 동기화 실행
     * @param channelId 동기화할 채널 ID (비어있으면 동작하지 않음)
     */
    fun syncIncremental(channelId: String = "") {
        viewModelScope.launch {
            // channelId가 비어있으면 동작하지 않음
            if (channelId.isBlank()) {
                _syncStatus.value = "❌ 채널 ID를 입력해주세요"
                Log.w("DevMenuViewModel-Sync", "❌ syncIncremental 호출됨 but channelId is blank")
                return@launch
            }
            
            _isSyncing.value = true

            // 스트림명 결정
            val streamName = "messages-$channelId"
            
            _syncStatus.value = "🔄 증분 동기화 시작..."

            Log.d("DevMenuViewModel-Sync", "🔄 Incremental sync started")
            Log.d("DevMenuViewModel-Sync", "   - Channel ID: $channelId")
            Log.d("DevMenuViewModel-Sync", "   - Stream Name: $streamName")

            try {
                val result = syncUseCase(streamName)
                when (result) {
                    is CustomResult.Success -> {
                        _syncStatus.value = "✅ 증분 동기화 완료"
                        Log.d("DevMenuViewModel-Sync", "✅ Incremental sync completed successfully")
                    }
                    is CustomResult.Failure -> {
                        _syncStatus.value = "❌ 증분 동기화 실패: ${result.error.message}"
                        Log.e("DevMenuViewModel-Sync", "❌ Incremental sync failed", result.error)
                    }
                    else -> {
                        _syncStatus.value = "⚠️ 증분 동기화 결과 알 수 없음"
                        Log.w(
                            "DevMenuViewModel-Sync",
                            "⚠️ Unknown result from incremental sync: $result"
                        )
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = "❌ 증분 동기화 중 오류 발생: ${e.message}"
                Log.e("DevMenuViewModel-Sync", "❌ Unexpected error during incremental sync", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * 캐시 클리어 + 전체 동기화 실행
     * @param tableName 동기화할 테이블명 (기본값: "messages")
     * @param channelId 클리어할 채널 ID (messages 테이블 전용)
     */
    fun resetAndSync(tableName: String = "messages", channelId: String = TEST_ROOM_ID) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "📱 로컬 캐시 클리어 시작..."

            Log.d(
                "DevMenuViewModel-Sync",
                "🚀 Reset and sync started for table: $tableName, channel: $channelId"
            )
            addMessage("🚀 로컬 캐시 클리어 + 동기화 시작 (채널: $channelId)")

            try {
                _syncStatus.value = "🗑️ 로컬 캐시 삭제 중..."
                addMessage("🗑️ 로컬 캐시 삭제 중...")

                when (val result = resetAndSyncUseCase(tableName, channelId)) {
                    is CustomResult.Success -> {
                        _syncStatus.value = "✅ 리셋 및 동기화 완료!"
                        Log.d("DevMenuViewModel-Sync", "✅ Reset and sync completed successfully")
                        Log.d("DevMenuViewModel-Sync", "   - Table: $tableName")
                        Log.d("DevMenuViewModel-Sync", "   - Channel: $channelId")
                        Log.d(
                            "DevMenuViewModel-Sync",
                            "   - Local cache cleared and remote data synced"
                        )
                        addMessage("✅ 리셋 및 동기화 완료! (테이블: $tableName, 채널: $channelId)")
                    }

                    is CustomResult.Failure -> {
                        _syncStatus.value = "❌ 리셋 및 동기화 실패: ${result.error.message}"
                        Log.e("DevMenuViewModel-Sync", "❌ Reset and sync failed", result.error)
                        Log.e("DevMenuViewModel-Sync", "   - Table: $tableName")
                        Log.e("DevMenuViewModel-Sync", "   - Channel: $channelId")
                        Log.e("DevMenuViewModel-Sync", "   - Error: ${result.error.message}")
                        addMessage("❌ 리셋 및 동기화 실패: ${result.error.message}")
                    }

                    else -> {
                        _syncStatus.value = "⚠️ 리셋 및 동기화 결과 알 수 없음"
                        Log.w(
                            "DevMenuViewModel-Sync",
                            "⚠️ Unknown result from reset and sync: $result"
                        )
                        addMessage("⚠️ 리셋 및 동기화 결과 알 수 없음")
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = "❌ 리셋 및 동기화 예외: ${e.message}"
                Log.e("DevMenuViewModel-Sync", "💥 Exception during reset and sync", e)
                Log.e("DevMenuViewModel-Sync", "   - Table: $tableName")
                Log.e("DevMenuViewModel-Sync", "   - Channel: $channelId")
                Log.e(
                    "DevMenuViewModel-Sync",
                    "   - Exception: ${e.javaClass.simpleName}: ${e.message}"
                )
                addMessage("💥 리셋 및 동기화 예외: ${e.message}")
            } finally {
                _isSyncing.value = false
                Log.d("DevMenuViewModel-Sync", "🏁 Reset and sync process finished")
            }
        }
    }

    // ================================
    // Room DB 검사 기능
    // ================================

    /**
     * 전체 Room DB 상태를 검사합니다
     */
    fun inspectFullDatabase() {
        viewModelScope.launch {
            _isDbInspecting.value = true
            _dbInspectionResult.value = "🔍 전체 데이터베이스 검사 중..."

            try {
                Log.d("DevMenuViewModel-DB", "🔍 Starting full database inspection")

                // RoomDatabaseLogger를 통해 전체 DB 상태 출력
                roomDatabaseLogger.logDatabaseState()

                _dbInspectionResult.value = "✅ 전체 DB 검사 완료! 로그를 확인하세요."
                addMessage("✅ 전체 DB 검사 완료! 자세한 내용은 로그 확인")

            } catch (e: Exception) {
                _dbInspectionResult.value = "❌ DB 검사 실패: ${e.message}"
                Log.e("DevMenuViewModel-DB", "❌ Database inspection failed", e)
                addMessage("❌ DB 검사 실패: ${e.message}")
            } finally {
                _isDbInspecting.value = false
            }
        }
    }

    /**
     * 특정 채널의 메시지를 검사합니다
     */
    fun inspectChannelMessages(channelId: String) {
        if (channelId.isBlank()) {
            _dbInspectionResult.value = "❌ 채널 ID를 입력해주세요"
            return
        }

        viewModelScope.launch {
            _isDbInspecting.value = true
            _dbInspectionResult.value = "🔍 채널 '$channelId' 메시지 검사 중..."
            _selectedChannelId.value = channelId

            try {
                Log.d("DevMenuViewModel-DB", "🔍 Starting channel inspection for: $channelId")

                // 채널별 메시지 상태 출력 (최근 20개)
                roomDatabaseLogger.logChannelMessages(channelId, 20)

                _dbInspectionResult.value = "✅ 채널 '$channelId' 검사 완료! 로그를 확인하세요."
                addMessage("✅ 채널 '$channelId' 검사 완료!")

            } catch (e: Exception) {
                _dbInspectionResult.value = "❌ 채널 검사 실패: ${e.message}"
                Log.e("DevMenuViewModel-DB", "❌ Channel inspection failed", e)
                addMessage("❌ 채널 검사 실패: ${e.message}")
            } finally {
                _isDbInspecting.value = false
            }
        }
    }

    /**
     * 메시지 테이블 상태만 검사합니다
     */
    fun inspectMessagesTable() {
        viewModelScope.launch {
            _isDbInspecting.value = true
            _dbInspectionResult.value = "🔍 메시지 테이블 검사 중..."

            try {
                Log.d("DevMenuViewModel-DB", "🔍 Starting messages table inspection")

                // 메시지 테이블 상세 정보 출력
                roomDatabaseLogger.logTableDetails("messages")

                _dbInspectionResult.value = "✅ 메시지 테이블 검사 완료! 로그를 확인하세요."
                addMessage("✅ 메시지 테이블 검사 완료!")

            } catch (e: Exception) {
                _dbInspectionResult.value = "❌ 테이블 검사 실패: ${e.message}"
                Log.e("DevMenuViewModel-DB", "❌ Messages table inspection failed", e)
                addMessage("❌ 테이블 검사 실패: ${e.message}")
            } finally {
                _isDbInspecting.value = false
            }
        }
    }

    /**
     * OutBox 테이블 상태를 검사합니다 (동기화 대기열)
     */
    fun inspectOutboxTable() {
        viewModelScope.launch {
            _isDbInspecting.value = true
            _dbInspectionResult.value = "🔍 OutBox 테이블 검사 중..."

            try {
                Log.d("DevMenuViewModel-DB", "🔍 Starting outbox table inspection")

                // OutBox 테이블 상세 정보 출력
                roomDatabaseLogger.logTableDetails("outboxRecord")

                _dbInspectionResult.value = "✅ OutBox 테이블 검사 완료! 로그를 확인하세요."
                addMessage("✅ OutBox 테이블 검사 완료!")

            } catch (e: Exception) {
                _dbInspectionResult.value = "❌ OutBox 검사 실패: ${e.message}"
                Log.e("DevMenuViewModel-DB", "❌ Outbox table inspection failed", e)
                addMessage("❌ OutBox 검사 실패: ${e.message}")
            } finally {
                _isDbInspecting.value = false
            }
        }
    }

    /**
     * 동기화 메타데이터 테이블을 검사합니다
     */
    fun inspectSyncMetadata() {
        viewModelScope.launch {
            _isDbInspecting.value = true
            _dbInspectionResult.value = "🔍 동기화 메타데이터 검사 중..."

            try {
                Log.d("DevMenuViewModel-DB", "🔍 Starting sync metadata inspection")

                // 동기화 메타데이터 테이블 정보 출력
                roomDatabaseLogger.logTableDetails("syncMetadata")

                _dbInspectionResult.value = "✅ 동기화 메타데이터 검사 완료! 로그를 확인하세요."
                addMessage("✅ 동기화 메타데이터 검사 완료!")

            } catch (e: Exception) {
                _dbInspectionResult.value = "❌ 메타데이터 검사 실패: ${e.message}"
                Log.e("DevMenuViewModel-DB", "❌ Sync metadata inspection failed", e)
                addMessage("❌ 메타데이터 검사 실패: ${e.message}")
            } finally {
                _isDbInspecting.value = false
            }
        }
    }

    /**
     * 동기화 실행 전후 DB 상태를 비교합니다
     */
    fun syncWithDbComparison(channelId: String) {
        if (channelId.isBlank()) {
            _dbInspectionResult.value = "❌ 채널 ID를 입력해주세요"
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            _isDbInspecting.value = true
            _syncStatus.value = "🔄 동기화 전후 DB 상태 비교 중..."

            try {
                Log.d("DevMenuViewModel-Sync", "🔍 === SYNC WITH DB COMPARISON START ===")
                Log.d("DevMenuViewModel-Sync", "📋 Channel: $channelId")

                // 동기화 실행 전 상태
                Log.d("DevMenuViewModel-Sync", "📊 === DB STATE BEFORE SYNC ===")
                roomDatabaseLogger.logChannelMessages(channelId, 10)
                roomDatabaseLogger.logTableState("outboxRecord")

                // 동기화 실행
                _syncStatus.value = "🔄 동기화 실행 중..."
                val streamName = "messages-$channelId"

                when (val result = syncUseCase(streamName)) {
                    is CustomResult.Success -> {
                        Log.d("DevMenuViewModel-Sync", "✅ Sync completed successfully")

                        // 동기화 실행 후 상태
                        Log.d("DevMenuViewModel-Sync", "📊 === DB STATE AFTER SYNC ===")
                        roomDatabaseLogger.logChannelMessages(channelId, 10)
                        roomDatabaseLogger.logTableState("outboxRecord")

                        _syncStatus.value = "✅ 동기화 및 DB 비교 완료!"
                        _dbInspectionResult.value = "✅ 동기화 전후 상태 비교 완료! 로그 확인"
                        addMessage("✅ 채널 '$channelId' 동기화 및 DB 비교 완료!")

                    }

                    is CustomResult.Failure -> {
                        _syncStatus.value = "❌ 동기화 실패: ${result.error.message}"
                        _dbInspectionResult.value = "❌ 동기화 실패: ${result.error.message}"
                        Log.e("DevMenuViewModel-Sync", "❌ Sync failed", result.error)
                        addMessage("❌ 동기화 실패: ${result.error.message}")
                    }

                    else -> {
                        _syncStatus.value = "⚠️ 동기화 결과 불명"
                        _dbInspectionResult.value = "⚠️ 동기화 결과 불명"
                        addMessage("⚠️ 동기화 결과 불명")
                    }
                }

                Log.d("DevMenuViewModel-Sync", "🔍 === SYNC WITH DB COMPARISON END ===")

            } catch (e: Exception) {
                _syncStatus.value = "❌ 동기화 비교 실패: ${e.message}"
                _dbInspectionResult.value = "❌ 동기화 비교 실패: ${e.message}"
                Log.e("DevMenuViewModel-Sync", "❌ Sync with comparison failed", e)
                addMessage("❌ 동기화 비교 실패: ${e.message}")
            } finally {
                _isSyncing.value = false
                _isDbInspecting.value = false
            }
        }
    }

    /**
     * 채널 ID 설정
     */
    fun setChannelId(channelId: String) {
        _selectedChannelId.value = channelId
    }
}
