package com.example.websocket.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain_repository.base.AuthRepository
import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.core.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global WebSocket service that manages a single WebSocket connection throughout the app lifecycle.
 * This service automatically handles:
 * - Authentication state monitoring and token refresh
 * - Network connectivity monitoring
 * - Automatic reconnection with proper credentials
 */
@Singleton
class GlobalWebSocketService @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val authRepository: AuthRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Connection configuration
    private var serverUrl: String? = null
    private var currentAuthToken: String? = null
    private var isServiceActive = false

    // Reconnection management with exponential backoff
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private val baseReconnectDelayMs = 1000L // 1초
    private val maxReconnectDelayMs = 60000L // 60초
    private var manuallyDisconnected = false
    private var reconnectJob: Job? = null

    // State management
    private val _globalConnectionState = MutableStateFlow<WebSocketConnectionState>(
        WebSocketConnectionState.Disconnected
    )
    val globalConnectionState: StateFlow<WebSocketConnectionState> =
        _globalConnectionState.asStateFlow()

    private val _isInForeground = MutableStateFlow(true)
    val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

    // Authentication monitoring
    private var authMonitoringJob: Job? = null
    private var connectionMaintenanceJob: Job? = null

    // Token refresh state
    private var currentUserSession: UserSession? = null
    private var isRefreshingToken = false

    init {
        // Start connection state monitoring
        startConnectionStateMonitoring()
    }

    /**
     * Initialize the service with authentication monitoring
     */
    fun initializeWithAuth(authStateFlow: Flow<CustomResult<UserSession, Exception>>) {

        authMonitoringJob?.cancel()
        authMonitoringJob = scope.launch {
            authStateFlow.collectLatest { authResult ->
                when (authResult) {
                    is CustomResult.Success -> {
                        currentUserSession = authResult.data
                        extractAndUpdateAuthToken(authResult.data)
                    }

                    is CustomResult.Failure -> {
                        currentUserSession = null
                        handleAuthenticationFailure()
                    }

                    else -> {
                        // Other authentication states
                    }
                }
            }
        }
    }

    /**
     * Configure the WebSocket connection
     */
    fun configure(serverUrl: String = WebSocketManager.SERVER_URL) {
        this.serverUrl = serverUrl
        Log.d(TAG, "GlobalWebSocketService configured with server: $serverUrl")

        // If we have auth token and are in foreground, connect
        if (currentAuthToken != null && _isInForeground.value) {
            scope.launch {
                attemptConnection()
            }
        }
    }

    /**
     * Start the service (typically called from Application.onCreate)
     */
    fun startService() {
        if (isServiceActive) {
            Log.d(TAG, "Service already active")
            return
        }

        isServiceActive = true
        Log.d(TAG, "GlobalWebSocketService started")

        // Start connection maintenance
        startConnectionMaintenance()
    }

    /**
     * Stop the service (typically called from Application.onTerminate or onDestroy)
     */
    fun stopService() {
        if (!isServiceActive) return

        isServiceActive = false
        Log.d(TAG, "GlobalWebSocketService stopped")

        // Cancel all jobs
        authMonitoringJob?.cancel()
        connectionMaintenanceJob?.cancel()
        reconnectJob?.cancel()

        // Disconnect WebSocket
        scope.launch {
            webSocketManager.disconnect()
        }
    }

    /**
     * Get the underlying WebSocketManager for sending messages
     */
    fun getWebSocketManager(): WebSocketManager = webSocketManager

    fun onAppForegrounded() {
        Log.d(TAG, "App entered foreground")
        _isInForeground.value = true
        manuallyDisconnected = false // Reset manual disconnect when app comes to foreground

        if (isServiceActive && currentAuthToken != null && serverUrl != null) {
            scope.launch {
                attemptConnection()
            }
        }
    }

    fun onAppBackgrounded() {
        Log.d(TAG, "App entered background")
        _isInForeground.value = false

        // Optionally disconnect in background to save resources
        // For chat apps, you might want to keep connection alive for push notifications
        // For now, we'll keep the connection alive but log the state change
        Log.d(TAG, "Maintaining WebSocket connection in background for real-time updates")
    }

    private fun startConnectionStateMonitoring() {
        scope.launch {
            webSocketManager.connectionState.collect { state ->
                _globalConnectionState.value = state
                Log.d(TAG, "Global connection state updated: ${state::class.simpleName}")
            }
        }
    }

    private fun startConnectionMaintenance() {
        connectionMaintenanceJob?.cancel()
        connectionMaintenanceJob = scope.launch {
            // Monitor connection state and handle reconnection
            combine(
                _isInForeground,
                _globalConnectionState
            ) { isInForeground, connectionState ->
                Pair(isInForeground, connectionState)
            }.collectLatest { (isInForeground, connectionState) ->

                when {
                    // App in foreground, should be connected, but disconnected
                    isInForeground &&
                            connectionState is WebSocketConnectionState.Disconnected &&
                            currentAuthToken != null &&
                            serverUrl != null -> {
                        Log.d(TAG, "App in foreground but disconnected, attempting reconnection")
                        delay(1000) // Brief delay before reconnection
                        attemptConnection()
                    }

                    // Connection error with valid auth - try to reconnect
                    connectionState is WebSocketConnectionState.Error &&
                            currentAuthToken != null &&
                            serverUrl != null &&
                            !connectionState.message.contains("Authentication") -> {
                        Log.d(TAG, "Connection error detected, scheduling reconnection")
                        scheduleReconnect()
                    }

                    // Handle disconnection by scheduling reconnect
                    connectionState is WebSocketConnectionState.Disconnected &&
                            !manuallyDisconnected &&
                            currentAuthToken != null &&
                            serverUrl != null -> {
                        Log.d(TAG, "Unexpected disconnection, scheduling reconnection")
                        scheduleReconnect()
                    }
                }
            }
        }
    }

    private suspend fun attemptConnection(): Result<Unit>? {
        val url = serverUrl

        // Check token validity before attempting connection
        val session = currentUserSession
        if (session == null) {
            Log.w(TAG, "Cannot connect: no user session available")
            return null
        }

        if (!session.hasValidToken()) {
            Log.w(TAG, "Cannot connect: token is invalid or expired")
            return null
        }

        // Refresh token if expiring soon (on-demand check)
        if (session.isTokenExpiringSoon()) {
            Log.i(TAG, "Token expiring soon, refreshing before connection")
            refreshTokenIfNeeded(session)
        }

        val token = currentAuthToken
        if (url == null || token == null) {
            Log.w(TAG, "Cannot connect: missing server URL or auth token")
            return null
        }

        // 연결 중이거나 이미 연결된 경우 중복 방지
        val currentState = webSocketManager.connectionState.value
        if (currentState is WebSocketConnectionState.Connected) {
            Log.d(TAG, "Already connected, skipping connection attempt")
            return Result.success(Unit)
        }
        if (currentState is WebSocketConnectionState.Connecting) {
            Log.d(TAG, "Connection already in progress, skipping duplicate attempt")
            return null
        }

        Log.d(TAG, "Attempting WebSocket connection to: $url")

        return try {
            val result = webSocketManager.connect(url, token)

            if (result.isSuccess) {
                Log.d(TAG, "WebSocket connection successful")

                // 연결 성공 후 인증 대기
                val authTimeout = withTimeoutOrNull(15000) { // 15초 대기
                    webSocketManager.isAuthenticated.first { it }
                }

                if (authTimeout == true) {
                    Log.d(TAG, "WebSocket authentication successful")
                } else {
                    Log.w(TAG, "WebSocket authentication timeout or failed")
                }
                result
            } else {
                Log.w(TAG, "WebSocket connection failed: ${result.exceptionOrNull()?.message}")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebSocket connection", e)
            Result.failure(e)
        }
    }

    private fun extractAndUpdateAuthToken(userSession: UserSession) {
        Log.d(TAG, "🔑 Starting token extraction and update process")
        Log.d(
            TAG,
            "Current token status: ${if (currentAuthToken != null) "has token" else "no token"}"
        )
        Log.d(TAG, "Service active: $isServiceActive, In foreground: ${_isInForeground.value}")

        // Check token validity first
        if (!userSession.hasValidToken()) {
            Log.w(TAG, "⚠️ UserSession has invalid or expired token")
            if (userSession.isTokenExpired()) {
                Log.w(TAG, "❌ Token is expired - requiring token refresh")
            }
            currentAuthToken = null
            return
        }

        // Check and refresh token if expiring soon (on-demand)
        if (userSession.isTokenExpiringSoon()) {
            val remainingTime = userSession.getTokenRemainingTimeSeconds() ?: 0
            Log.w(
                TAG,
                "⏰ Token will expire soon (${remainingTime}s remaining) - triggering refresh"
            )
            scope.launch {
                refreshTokenIfNeeded(userSession)
            }
        }

        val newToken: String? = userSession.idToken?.value

        when {
            newToken != null && newToken != currentAuthToken -> {
                Log.i(TAG, "✅ Auth token updated successfully")
                Log.d(
                    TAG,
                    "New token preview: ${newToken.substring(0, minOf(15, newToken.length))}..."
                )
                Log.d(TAG, "Previous token: ${if (currentAuthToken != null) "existed" else "none"}")

                val remainingTime = userSession.getTokenRemainingTimeSeconds()
                if (remainingTime != null) {
                    Log.d(
                        TAG,
                        "Token valid for ${remainingTime}s (${remainingTime / 60}m ${remainingTime % 60}s)"
                    )
                }

                currentAuthToken = newToken

                // If we're in foreground and have server URL, connect/reconnect
                if (_isInForeground.value && serverUrl != null && isServiceActive) {
                    Log.d(TAG, "🚀 Triggering WebSocket connection due to token update")
                    scope.launch {
                        val result = attemptConnection()
                        Log.d(
                            TAG,
                            "Connection attempt result: ${if (result?.isSuccess == true) "success" else "failed"}"
                        )
                    }
                } else {
                    Log.w(
                        TAG,
                        "⏸️ Not connecting - foreground: ${_isInForeground.value}, serverUrl: ${serverUrl != null}, serviceActive: $isServiceActive"
                    )
                }
            }

            newToken == null -> {
                Log.e(TAG, "❌ No valid auth token found in auth data")
                Log.d(TAG, "This may indicate authentication failure or token extraction issues")
            }

            newToken == currentAuthToken -> {
                Log.d(TAG, "🔄 Token unchanged, skipping update")
            }
        }

        Log.d(TAG, "🏁 Token extraction and update process completed")
    }


    private fun handleAuthenticationFailure() {
        currentAuthToken = null
        scope.launch {
            webSocketManager.disconnect()
        }
    }

    /**
     * Force reconnection with state reset (unified method)
     */
    fun forceReconnect() {
        scope.launch {
            Log.d(TAG, "Force reconnect with state reset requested")

            // Reset reconnection state first
            resetReconnectionState()

            // Disconnect if currently connected
            if (webSocketManager.connectionState.value is WebSocketConnectionState.Connected) {
                webSocketManager.disconnect()
                // Wait for disconnection
                withTimeoutOrNull(3000) {
                    webSocketManager.connectionState.first { it is WebSocketConnectionState.Disconnected }
                }
            }

            // Attempt reconnection if credentials available
            if (currentAuthToken != null && serverUrl != null) {
                delay(1000) // Brief delay before reconnection
                attemptConnection()
            } else {
                Log.w(TAG, "Cannot reconnect: missing credentials")
            }
        }
    }

    /**
     * Manual disconnection
     */
    fun forceDisconnect() {
        manuallyDisconnected = true
        reconnectJob?.cancel()
        scope.launch {
            webSocketManager.disconnect()
        }
    }

    private fun scheduleReconnect() {
        // Don't reconnect if manually disconnected or service not active
        if (manuallyDisconnected || !isServiceActive) {
            Log.d(TAG, "Skipping reconnection - manually disconnected or service inactive")
            return
        }

        // Check if we've exceeded max attempts
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "Maximum reconnection attempts ($maxReconnectAttempts) exceeded")
            _globalConnectionState.value = WebSocketConnectionState.Error(
                message = "Connection lost - maximum reconnection attempts exceeded",
                throwable = Exception("Auto-reconnection failed after $maxReconnectAttempts attempts")
            )
            return
        }

        // Cancel any existing reconnection job
        reconnectJob?.cancel()

        reconnectJob = scope.launch {
            // Calculate delay with exponential backoff: base * 2^attempts, capped at max
            val delay = minOf(
                baseReconnectDelayMs * (1L shl reconnectAttempts.coerceAtMost(6)), // 최대 2^6 = 64배까지
                maxReconnectDelayMs
            )

            reconnectAttempts++

            Log.d(TAG, "Scheduling reconnection attempt #$reconnectAttempts in ${delay}ms")

            delay(delay)

            // Check if we're still supposed to reconnect
            if (manuallyDisconnected || !isServiceActive ||
                _globalConnectionState.value is WebSocketConnectionState.Connected
            ) {
                Log.d(TAG, "Cancelling reconnection - state changed")
                return@launch
            }

            Log.d(TAG, "Attempting reconnection #$reconnectAttempts")

            try {
                attemptConnection()
                if (webSocketManager.connectionState.value is WebSocketConnectionState.Connected) {
                    Log.d(TAG, "Auto-reconnection successful on attempt #$reconnectAttempts")
                    reconnectAttempts = 0 // Reset on success

                    // Re-join previously joined rooms if needed
                    rejoinRoomsAfterReconnection()

                } else {
                    Log.w(TAG, "Auto-reconnection attempt #$reconnectAttempts failed")
                    // Schedule next attempt
                    scheduleReconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during auto-reconnection attempt #$reconnectAttempts", e)
                scheduleReconnect()
            }
        }
    }

    private suspend fun rejoinRoomsAfterReconnection() {
        // This could be enhanced to track joined rooms if needed
        // For now, leave it as a placeholder for room re-joining logic
        Log.d(TAG, "Room re-joining after reconnection (placeholder)")
    }

    /**
     * Reset reconnection state for manual retry
     */
    fun resetReconnectionState() {
        Log.d(TAG, "Resetting reconnection state for manual retry")
        manuallyDisconnected = false
        reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null
    }

    /**
     * Refresh token if needed and not already in progress
     */
    private suspend fun refreshTokenIfNeeded(userSession: UserSession) {
        if (isRefreshingToken) {
            Log.d(TAG, "Token refresh already in progress, skipping")
            return
        }

        if (!userSession.isTokenExpiringSoon(thresholdMinutes = 10)) {
            Log.d(TAG, "Token is still valid for more than 10 minutes, skipping refresh")
            return
        }

        isRefreshingToken = true
        Log.i(TAG, "🔄 Starting automatic token refresh")

        try {
            when (val refreshResult = authRepository.refreshToken()) {
                is CustomResult.Success -> {
                    val newSession = refreshResult.data
                    currentUserSession = newSession

                    val newToken = newSession.idToken?.value
                    if (newToken != null && newToken != currentAuthToken) {
                        Log.i(TAG, "✅ Token refresh successful")
                        currentAuthToken = newToken

                        val remainingTime = newSession.getTokenRemainingTimeSeconds() ?: 0
                        Log.d(
                            TAG,
                            "New token valid for ${remainingTime}s (${remainingTime / 60}m ${remainingTime % 60}s)"
                        )

                        // Reconnect WebSocket with new token if needed
                        if (_globalConnectionState.value !is WebSocketConnectionState.Connected) {
                            Log.d(TAG, "🚀 Reconnecting WebSocket with refreshed token")
                            attemptConnection()
                        }
                    } else {
                        Log.w(TAG, "⚠️ Token refresh returned same or null token")
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(
                        TAG,
                        "❌ Token refresh failed: ${refreshResult.error.message}",
                        refreshResult.error
                    )
                    // Don't retry immediately to avoid infinite loops
                    delay(60_000) // Wait 1 minute before allowing next refresh attempt
                }

                else -> {
                    Log.e(TAG, "❌ Unexpected token refresh result: $refreshResult")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during token refresh", e)
        } finally {
            isRefreshingToken = false
        }
    }

    companion object {
        private const val TAG = "GlobalWebSocketService"
    }
}