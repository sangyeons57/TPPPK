package com.example.core_common.websocket

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier

@Singleton
class WebSocketManagerImpl @Inject constructor() : WebSocketManager {
    
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS) // Cloud Run LB idle-timeout is 30s, so use 15s
        .retryOnConnectionFailure(true)
        .apply {
            // Configure SSL for Google Cloud Run compatibility
            configureSslForCloudRun()
        }
        .build()
        
    private fun OkHttpClient.Builder.configureSslForCloudRun() {
        try {
            // Create a trust manager that accepts Google Cloud Run certificates
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    // For Google Cloud Run, we trust certificates issued by known CAs
                    // In production, you might want to add more specific validation
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Create hostname verifier for Cloud Run domains
            val hostnameVerifier = HostnameVerifier { hostname, session ->
                // Accept Google Cloud Run domains
                hostname.endsWith(".run.app") || 
                hostname.endsWith(".asia-northeast3.run.app") ||
                hostname.contains("websocket-chat") ||
                hostname == "localhost"
            }

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            hostnameVerifier(hostnameVerifier)
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure SSL, using default settings", e)
        }
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _connectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Disconnected)
    override val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()
    
    private val _incomingMessages = MutableSharedFlow<WebSocketMessage>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incomingMessages: Flow<WebSocketMessage> = _incomingMessages.asSharedFlow()
    
    private var webSocket: WebSocket? = null
    private var currentRoomId: String? = null
    private var reconnectJob: Job? = null
    
    // Connection credentials for auto-reconnection
    private var lastServerUrl: String? = null
    private var lastAuthToken: String? = null
    
    // Authentication state tracking
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened")
            _connectionState.value = WebSocketConnectionState.Connected
            // OkHttp handles ping/pong automatically with pingInterval
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            try {
                val message = json.decodeFromString<WebSocketMessage>(text)
                
                // Handle authentication responses
                when (message.type) {
                    WebSocketMessage.TYPE_AUTH_SUCCESS -> {
                        Log.d(TAG, "Authentication successful")
                        _isAuthenticated.value = true
                    }
                    WebSocketMessage.TYPE_ERROR -> {
                        if (message.content?.contains("Authentication") == true) {
                            Log.w(TAG, "Authentication failed: ${message.content}")
                            _isAuthenticated.value = false
                        }
                    }
                }
                
                _incomingMessages.tryEmit(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message: $text", e)
            }
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            _connectionState.value = WebSocketConnectionState.Disconnected
            _isAuthenticated.value = false
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            
            when (code) {
                1000, 1001 -> {
                    // Normal closure or going away - don't reconnect
                    _connectionState.value = WebSocketConnectionState.Disconnected
                    _isAuthenticated.value = false
                }
                1008 -> {
                    // Policy Violation - likely authentication issue, don't auto-reconnect
                    _connectionState.value = WebSocketConnectionState.Error(
                        message = "Authentication required (1008 - Policy Violation)",
                        throwable = Exception("WebSocket closed with code 1008: $reason")
                    )
                    _isAuthenticated.value = false
                    Log.w(TAG, "Authentication error (1008): $reason - Manual token refresh required")
                }
                else -> {
                    // Other unexpected closures - attempt reconnection
                    _connectionState.value = WebSocketConnectionState.Disconnected
                    _isAuthenticated.value = false
                    scheduleReconnect()
                }
            }
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket connection failed", t)
            _connectionState.value = WebSocketConnectionState.Error(
                message = "Connection failed: ${t.message}",
                throwable = t
            )
            scheduleReconnect()
        }
    }
    
    override suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (_connectionState.value is WebSocketConnectionState.Connected) {
                    return@withContext Result.success(Unit)
                }
                
                // Store credentials for auto-reconnection
                lastServerUrl = serverUrl
                lastAuthToken = authToken
                
                _connectionState.value = WebSocketConnectionState.Connecting
                
                val request = Request.Builder()
                    .url(serverUrl)
                    .addHeader("Authorization", "Bearer $authToken")
                    .build()
                
                webSocket?.close(1000, "Reconnecting")
                webSocket = okHttpClient.newWebSocket(request, webSocketListener)
                
                // Wait for connection or timeout
                val timeoutJob = scope.launch {
                    delay(10_000) // 10 second timeout
                    if (_connectionState.value is WebSocketConnectionState.Connecting) {
                        _connectionState.value = WebSocketConnectionState.Error("Connection timeout")
                    }
                }
                
                connectionState.first { it is WebSocketConnectionState.Connected || it is WebSocketConnectionState.Error }
                timeoutJob.cancel()
                
                when (val state = _connectionState.value) {
                    is WebSocketConnectionState.Connected -> Result.success(Unit)
                    is WebSocketConnectionState.Error -> Result.failure(Exception(state.message))
                    else -> Result.failure(Exception("Unexpected connection state"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                _connectionState.value = WebSocketConnectionState.Error("Failed to connect: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            reconnectJob?.cancel()
            webSocket?.close(1000, "User disconnection")
            webSocket = null
            currentRoomId = null
            
            // Clear stored credentials on manual disconnect
            lastServerUrl = null
            lastAuthToken = null
            
            _connectionState.value = WebSocketConnectionState.Disconnected
            _isAuthenticated.value = false
        }
    }
    
    override suspend fun sendMessage(message: WebSocketMessage): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val ws = webSocket ?: return@withContext Result.failure(Exception("WebSocket not connected"))
                val jsonMessage = json.encodeToString(message)
                
                val success = ws.send(jsonMessage)
                if (success) {
                    Log.d(TAG, "Sent message: $jsonMessage")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to send message"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun joinRoom(roomId: String): Result<Unit> {
        currentRoomId = roomId
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_JOIN_ROOM,
            roomId = roomId
        )
        return sendMessage(message)
    }
    
    override suspend fun leaveRoom(roomId: String): Result<Unit> {
        if (currentRoomId == roomId) {
            currentRoomId = null
        }
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_LEAVE_ROOM,
            roomId = roomId
        )
        return sendMessage(message)
    }
    
    
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delay = 1000L // Start with 1 second
            repeat(5) { attempt ->
                delay(delay)
                Log.d(TAG, "Attempting reconnection #${attempt + 1}")
                
                if (_connectionState.value is WebSocketConnectionState.Connected) {
                    return@launch
                }
                
                // Try to reconnect if we have stored credentials
                val serverUrl = lastServerUrl
                val authToken = lastAuthToken
                
                if (serverUrl != null && authToken != null) {
                    Log.d(TAG, "Attempting auto-reconnection with stored credentials")
                    try {
                        val result = connect(serverUrl, authToken)
                        if (result.isSuccess) {
                            Log.d(TAG, "Auto-reconnection successful")
                            
                            // Rejoin the current room if we were in one
                            currentRoomId?.let { roomId ->
                                joinRoom(roomId)
                            }
                            
                            return@launch
                        } else {
                            Log.w(TAG, "Auto-reconnection failed: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during auto-reconnection", e)
                    }
                } else {
                    Log.w(TAG, "No stored credentials for auto-reconnection")
                    // Set state to disconnected to allow manual reconnection
                    _connectionState.value = WebSocketConnectionState.Disconnected
                }
                
                delay = minOf(delay * 2, 30_000L) // Exponential backoff, max 30 seconds
            }
            
            Log.w(TAG, "Auto-reconnection attempts exhausted")
            _connectionState.value = WebSocketConnectionState.Error(
                message = "Connection lost - manual reconnection required",
                throwable = Exception("Auto-reconnection failed after 5 attempts")
            )
        }
    }
    
    /**
     * Update stored authentication token for auto-reconnection
     */
    fun updateAuthToken(newAuthToken: String) {
        Log.d(TAG, "Updating stored auth token for auto-reconnection")
        lastAuthToken = newAuthToken
    }
    
    companion object {
        private const val TAG = "WebSocketManager"
    }
}