package com.example.websocket

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.core_common.result.CustomResult
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 단순화된 WebSocket 동기화 서비스
 * Single source of truth 아키텍처에서 백그라운드 동기화만 담당
 * UI와 직접 연결되지 않고, Repository 패턴을 통해 Room DB와 동기화
 */
@Singleton
class GlobalWebSocketService @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "GlobalWebSocketService"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 연결 상태 (읽기 전용)
    private val _connectionState =
        MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Disconnected)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private var isServiceActive = false
    private var currentAuthToken: String? = null
    
    /**
     * 서비스 초기화 - MyApp에서 한 번만 호출
     */
    suspend fun initialize() {
        if (isServiceActive) {
            Log.d(TAG, "Service already active, skipping initialization")
            return
        }

        Log.i(TAG, "🚀 Starting WebSocket sync service initialization")
        
        isServiceActive = true

        // 인증 상태 모니터링 시작
        startAuthenticationMonitoring()

        // WebSocket 연결 상태 모니터링
        startConnectionStateMonitoring()

        Log.i(TAG, "✅ WebSocket sync service initialized")
    }
    
    /**
     * 인증 상태 변경 모니터링
     */
    private fun startAuthenticationMonitoring() {
        scope.launch {
            val authUseCases = authSessionUseCaseProvider.create()
            authUseCases.getCurrentUserSessionStreamUseCase().collectLatest { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val userSession = result.data
                        val newToken = userSession.idToken?.value

                        if (newToken != null && newToken != currentAuthToken) {
                            Log.d(TAG, "🔑 New authentication token received, updating connection")
                            currentAuthToken = newToken
                            attemptConnection()
                        } else if (newToken == null && currentAuthToken != null) {
                            Log.d(TAG, "🔓 User logged out, disconnecting")
                            currentAuthToken = null
                            disconnect()
                        }
                    }

                    is CustomResult.Failure -> {
                        Log.w(TAG, "Authentication failed: ${result.error}")
                        currentAuthToken = null
                        disconnect()
                    }

                    else -> {
                        Log.d(TAG, "Authentication state: $result")
                    }
                }
            }
        }
    }

    /**
     * WebSocket 연결 상태 모니터링
     */
    private fun startConnectionStateMonitoring() {
        scope.launch {
            webSocketManager.connectionState.collect { state ->
                _connectionState.value = state
                Log.d(TAG, "Connection state changed: $state")

                when (state) {
                    is WebSocketConnectionState.Connected -> {
                        Log.i(TAG, "✅ WebSocket connected, ready for sync")
                        // TODO: 여기서 Repository에게 동기화 시작 신호를 보낼 수 있음
                    }

                    is WebSocketConnectionState.Disconnected -> {
                        Log.w(TAG, "⚠️ WebSocket disconnected")
                        // 자동 재연결 시도 (인증 토큰이 있는 경우)
                        if (currentAuthToken != null) {
                            scheduleReconnect()
                        }
                    }

                    is WebSocketConnectionState.Error -> {
                        Log.e(TAG, "❌ WebSocket error: ${state.message}")
                        scheduleReconnect()
                    }

                    is WebSocketConnectionState.Connecting -> {
                        Log.d(TAG, "🔄 WebSocket connecting...")
                    }
                }
            }
        }
    }

    /**
     * WebSocket 연결 시도
     */
    private suspend fun attemptConnection() {
        if (currentAuthToken == null) {
            Log.w(TAG, "No auth token available, skipping connection")
            return
        }

        try {
            Log.d(TAG, "🔄 Attempting WebSocket connection...")
            val result = webSocketManager.connect(WebSocketManager.SERVER_URL, currentAuthToken!!)
            
            if (result.isSuccess) {
                Log.i(TAG, "✅ WebSocket connection successful")
            } else {
                Log.e(TAG, "❌ WebSocket connection failed: ${result.exceptionOrNull()?.message}")
                scheduleReconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during connection attempt", e)
            scheduleReconnect()
        }
    }

    /**
     * 재연결 스케줄링 (단순한 재시도)
     */
    private fun scheduleReconnect() {
        if (!isServiceActive || currentAuthToken == null) return

        scope.launch {
            Log.d(TAG, "⏰ Scheduling reconnect in 5 seconds...")
            kotlinx.coroutines.delay(5000) // 5초 후 재시도
            attemptConnection()
        }
    }

    /**
     * 연결 해제
     */
    private suspend fun disconnect() {
        try {
            webSocketManager.disconnect()
            Log.d(TAG, "🔌 WebSocket disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }
    
    /**
     * 서비스 종료
     */
    fun shutdown() {
        Log.i(TAG, "🛑 Shutting down WebSocket sync service")
        isServiceActive = false
        scope.launch {
            disconnect()
        }
    }

    // Lifecycle callbacks
    override fun onResume(owner: LifecycleOwner) {
        Log.d(TAG, "📱 App resumed, ensuring connection")
        if (currentAuthToken != null) {
            scope.launch { attemptConnection() }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "📱 App paused")
        // 연결은 유지하되 필요시 최적화 가능
    }

    // Legacy compatibility methods (기존 코드 호환성을 위해 유지)
    fun getWebSocketManager(): WebSocketManager = webSocketManager

    val globalConnectionState: StateFlow<WebSocketConnectionState> = connectionState

    suspend fun forceReconnect() {
        Log.d(TAG, "🔄 Force reconnect requested")
        attemptConnection()
    }
    
    fun forceDisconnect() {
        Log.d(TAG, "🔌 Force disconnect requested")
        scope.launch { disconnect() }
    }

    // Message sending functionality
    suspend fun sendMessage(message: WebSocketMessage): Result<Unit> {
        if (connectionState.value !is WebSocketConnectionState.Connected) {
            Log.w(TAG, "Cannot send message: WebSocket not connected")
            return Result.failure(Exception("WebSocket not connected"))
        }

        Log.d(TAG, "Sending message via WebSocket: ${message.type} to room ${message.roomId}")
        return webSocketManager.sendMessage(message)
    }

    // Room management stubs (향후 Repository에서 처리될 예정)
    suspend fun joinRoom(roomId: String): Result<Unit> {
        Log.d(TAG, "Room join requested: $roomId")
        if (connectionState.value !is WebSocketConnectionState.Connected) {
            Log.w(TAG, "Cannot join room: WebSocket not connected")
            return Result.failure(Exception("WebSocket not connected"))
        }

        return webSocketManager.joinRoom(roomId)
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> {
        Log.d(TAG, "Room leave requested: $roomId")
        if (connectionState.value !is WebSocketConnectionState.Connected) {
            Log.w(TAG, "Cannot leave room: WebSocket not connected")
            return Result.failure(Exception("WebSocket not connected"))
        }

        return webSocketManager.leaveRoom(roomId)
    }

    fun getJoinedRooms(): Set<String> {
        Log.d(TAG, "Joined rooms requested (will be handled by Repository)")
        return emptySet()
    }

    fun isJoinedToRoom(roomId: String): Boolean {
        Log.d(TAG, "Room join status requested: $roomId (will be handled by Repository)")
        return false
    }
}