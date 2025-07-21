package com.example.core_common.websocket

import android.util.Log
import com.example.core_common.result.CustomResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val webSocketManager: WebSocketManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Connection configuration
    private var serverUrl: String? = null
    private var currentAuthToken: String? = null
    private var isServiceActive = false
    
    // State management
    private val _globalConnectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Disconnected)
    val globalConnectionState: StateFlow<WebSocketConnectionState> = _globalConnectionState.asStateFlow()
    
    private val _isInForeground = MutableStateFlow(true)
    val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()
    
    // Authentication monitoring
    private var authMonitoringJob: Job? = null
    private var connectionMaintenanceJob: Job? = null
    
    init {
        // Start connection state monitoring
        startConnectionStateMonitoring()
        
        Log.d(TAG, "GlobalWebSocketService initialized")
    }
    
    /**
     * Initialize the service with authentication monitoring
     */
    fun initializeWithAuth(authStateFlow: Flow<CustomResult<*, *>>) {
        Log.d(TAG, "Initializing GlobalWebSocketService with authentication monitoring")
        
        authMonitoringJob?.cancel()
        authMonitoringJob = scope.launch {
            authStateFlow.collectLatest { authResult ->
                when (authResult) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "Authentication successful, extracting token")
                        extractAndUpdateAuthToken(authResult.data)
                    }
                    is CustomResult.Failure -> {
                        Log.w(TAG, "Authentication failed, disconnecting WebSocket")
                        handleAuthenticationFailure()
                    }
                    else -> {
                        Log.d(TAG, "Authentication state: ${authResult::class.simpleName}")
                    }
                }
            }
        }
    }
    
    /**
     * Configure the WebSocket connection
     */
    fun configure(serverUrl: String) {
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
                        Log.d(TAG, "Connection error detected, will retry after delay")
                        delay(5000) // Wait 5 seconds before retry on error
                        attemptConnection()
                    }
                }
            }
        }
    }
    
    private suspend fun attemptConnection() {
        val url = serverUrl
        val token = currentAuthToken
        
        if (url == null || token == null) {
            Log.w(TAG, "Cannot connect: missing server URL or auth token")
            return
        }
        
        if (webSocketManager.connectionState.value is WebSocketConnectionState.Connected) {
            Log.d(TAG, "Already connected, skipping connection attempt")
            return
        }
        
        Log.d(TAG, "Attempting WebSocket connection to: $url")
        
        try {
            val result = webSocketManager.connect(url, token)
            
            if (result.isSuccess) {
                Log.d(TAG, "WebSocket connection successful")
            } else {
                Log.w(TAG, "WebSocket connection failed: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebSocket connection", e)
        }
    }
    
    private fun extractAndUpdateAuthToken(authData: Any?) {
        Log.d(TAG, "Extracting auth token from: ${authData?.javaClass?.simpleName}")
        
        val newToken: String? = when {
            // Try direct access first (safer and more reliable)
            authData != null -> {
                extractTokenDirectly(authData) ?: extractTokenViaReflection(authData)
            }
            else -> {
                Log.w(TAG, "Auth data is null")
                null
            }
        }
        
        if (newToken != null && newToken != currentAuthToken) {
            Log.d(TAG, "Auth token updated (${newToken.substring(0, minOf(10, newToken.length))}...)")
            currentAuthToken = newToken
            
            // Update the WebSocketManager's stored token for auto-reconnection
            (webSocketManager as? WebSocketManagerImpl)?.updateAuthToken(newToken)
            
            // If we're in foreground and have server URL, connect/reconnect
            if (_isInForeground.value && serverUrl != null) {
                scope.launch {
                    attemptConnection()
                }
            }
        } else if (newToken == null) {
            Log.w(TAG, "No valid auth token found in auth data")
            // Log more details for debugging
            Log.d(TAG, "Auth data details: type=${authData?.javaClass?.name}, value=$authData")
        } else {
            Log.d(TAG, "Token unchanged, skipping update")
        }
    }
    
    /**
     * Direct token extraction without reflection (primary method)
     */
    private fun extractTokenDirectly(authData: Any?): String? {
        return try {
            // Cast to UserSession if possible and access token directly
            val userSessionClass = authData?.javaClass
            if (userSessionClass?.simpleName == "UserSession") {
                val idTokenField = userSessionClass.getDeclaredField("idToken")
                idTokenField.isAccessible = true
                val idToken = idTokenField.get(authData)
                
                if (idToken != null) {
                    // Get the token value directly without String reflection
                    val tokenClass = idToken.javaClass
                    val valueField = tokenClass.getDeclaredField("value")
                    valueField.isAccessible = true
                    val tokenValue = valueField.get(idToken)
                    
                    // The tokenValue should be a String, use it directly
                    val result = tokenValue?.toString()
                    if (result != null) {
                        Log.d(TAG, "Successfully extracted token directly (length: ${result.length})")
                        return result
                    } else {
                        Log.w(TAG, "Token value is null")
                        return null
                    }
                } else {
                    Log.w(TAG, "idToken field is null")
                    null
                }
            } else {
                Log.w(TAG, "Class name doesn't match UserSession: ${userSessionClass?.simpleName}")
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct extraction failed, will try reflection fallback: ${e.message}")
            null
        }
    }
    
    /**
     * Fallback token extraction via reflection with improved error handling
     * Avoids accessing String.value field which doesn't exist in Android 6.0+
     */
    private fun extractTokenViaReflection(authData: Any?): String? {
        return try {
            val userSessionClass = authData?.javaClass
            if (userSessionClass?.simpleName == "UserSession") {
                val idTokenField = userSessionClass.getDeclaredField("idToken")
                idTokenField.isAccessible = true
                val idToken = idTokenField.get(authData)
                
                if (idToken != null) {
                    // Try to get the value field from the Token class
                    val tokenClass = idToken.javaClass
                    try {
                        val valueField = tokenClass.getDeclaredField("value")
                        valueField.isAccessible = true
                        val tokenValue = valueField.get(idToken)
                        
                        // Use toString() to avoid String.value field access issues
                        val result = tokenValue?.toString()
                        if (result != null) {
                            Log.d(TAG, "Successfully extracted token via reflection (length: ${result.length})")
                            return result
                        }
                    } catch (e: NoSuchFieldException) {
                        Log.w(TAG, "No 'value' field in token class: ${tokenClass.simpleName}")
                        
                        // Try toString() as fallback
                        val result = idToken.toString()
                        if (result.isNotEmpty() && result != "null") {
                            Log.d(TAG, "Using toString() as token fallback (length: ${result.length})")
                            return result
                        }
                    }
                    
                    Log.w(TAG, "Could not extract token value")
                    null
                } else {
                    Log.w(TAG, "idToken field is null")
                    null
                }
            } else {
                Log.w(TAG, "Class name doesn't match UserSession: ${userSessionClass?.simpleName}")
                null
            }
        } catch (e: NoSuchFieldException) {
            Log.e(TAG, "Required field not found in UserSession class", e)
            null
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Cannot access UserSession fields", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during token extraction", e)
            null
        }
    }
    
    private fun handleAuthenticationFailure() {
        currentAuthToken = null
        scope.launch {
            webSocketManager.disconnect()
        }
    }
    
    /**
     * Manual connection control (for testing or specific use cases)
     */
    fun forceReconnect() {
        scope.launch {
            webSocketManager.disconnect()
            delay(1000)
            attemptConnection()
        }
    }
    
    /**
     * Manual disconnection
     */
    fun forceDisconnect() {
        scope.launch {
            webSocketManager.disconnect()
        }
    }
    
    companion object {
        private const val TAG = "GlobalWebSocketService"
    }
}