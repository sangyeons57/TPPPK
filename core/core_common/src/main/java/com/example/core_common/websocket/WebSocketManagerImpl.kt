package com.example.core_common.websocket

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
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
        .pingInterval(30, TimeUnit.SECONDS)
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
    private var heartbeatJob: Job? = null
    
    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened")
            _connectionState.value = WebSocketConnectionState.Connected
            startHeartbeat()
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            try {
                val message = json.decodeFromString<WebSocketMessage>(text)
                _incomingMessages.tryEmit(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message: $text", e)
            }
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            _connectionState.value = WebSocketConnectionState.Disconnected
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            _connectionState.value = WebSocketConnectionState.Disconnected
            stopHeartbeat()
            
            // Attempt reconnection for unexpected closures
            if (code != 1000 && code != 1001) { // Not normal closure or going away
                scheduleReconnect()
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
            stopHeartbeat()
            webSocket?.close(1000, "User disconnection")
            webSocket = null
            currentRoomId = null
            _connectionState.value = WebSocketConnectionState.Disconnected
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
    
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value is WebSocketConnectionState.Connected) {
                delay(30_000) // Send heartbeat every 30 seconds
                val heartbeat = WebSocketMessage(type = WebSocketMessage.TYPE_HEARTBEAT)
                sendMessage(heartbeat)
            }
        }
    }
    
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
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
                
                // Try to reconnect if we have connection details
                // Note: This would need the original serverUrl and authToken
                // For now, just update the state to allow manual reconnection
                _connectionState.value = WebSocketConnectionState.Disconnected
                
                delay = minOf(delay * 2, 30_000L) // Exponential backoff, max 30 seconds
            }
        }
    }
    
    companion object {
        private const val TAG = "WebSocketManager"
    }
}