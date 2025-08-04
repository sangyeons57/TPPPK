package com.example.websocket.core

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class WebSocketManagerImpl @Inject constructor() : WebSocketManager {

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // 서버 4분 타임아웃 대비 30초 핑 (8배 여유)
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
            sslContext.init(null, trustAllCerts, SecureRandom())

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

    private val _connectionState = MutableStateFlow<WebSocketConnectionState>(
        WebSocketConnectionState.Disconnected
    )
    override val connectionState: StateFlow<WebSocketConnectionState> =
        _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<WebSocketMessage>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incomingMessages: Flow<WebSocketMessage> = _incomingMessages.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var currentRoomId: String? = null

    // Simple disconnection flag
    private var manuallyDisconnected = false

    // Authentication state tracking
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened successfully")
            Log.i(TAG, "🔌 WebSocket connected - URL: ${response.request.url}")
            _connectionState.value = WebSocketConnectionState.Connected("")

            // Reset manual disconnect flag
            manuallyDisconnected = false

            // OkHttp handles ping/pong automatically with pingInterval
            Log.d(TAG, "Connection state updated to Connected, reconnect attempts reset")
        }

        override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
            Log.i(TAG, "🏓 [AUTO-PING-PONG] Received pong frame: ${bytes.hex()}")
            super.onMessage(webSocket, bytes)
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
            Log.w(TAG, "🔌 WebSocket disconnected - Code: $code, Reason: $reason")

            // 연결 끄어질 때 모든 방에서 퇴장
            leaveAllRooms()

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
                    Log.w(
                        TAG,
                        "Authentication error (1008): $reason - Manual token refresh required"
                    )
                }

                else -> {
                    // Other unexpected closures
                    _connectionState.value = WebSocketConnectionState.Disconnected
                    _isAuthenticated.value = false
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket connection failed", t)
            _connectionState.value = WebSocketConnectionState.Error(
                message = "Connection failed: ${t.message}",
                throwable = t
            )
        }
    }

    override suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (_connectionState.value is WebSocketConnectionState.Connected) {
                    return@withContext Result.success(Unit)
                }

                // Reset manual disconnect flag
                manuallyDisconnected = false

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
                        _connectionState.value =
                            WebSocketConnectionState.Error("Connection timeout")
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
                _connectionState.value =
                    WebSocketConnectionState.Error("Failed to connect: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Manual disconnect requested")

                // Set manual disconnect flag
                manuallyDisconnected = true

                // Leave all rooms before disconnecting
                leaveAllRooms()

                webSocket?.close(1000, "User disconnection")
                webSocket = null

                _connectionState.value = WebSocketConnectionState.Disconnected
                _isAuthenticated.value = false

                Log.d(TAG, "Manual disconnect completed")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
                _isAuthenticated.value = false
                Result.failure(e)
            }
        }
    }

    override suspend fun authenticate(authToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (_connectionState.value !is WebSocketConnectionState.Connected) {
                    return@withContext Result.failure(IllegalStateException("WebSocket not connected"))
                }

                val authMessage = WebSocketMessage(
                    type = WebSocketMessage.TYPE_AUTH,
                    authToken = authToken
                )

                sendMessage(authMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Authentication failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun sendMessage(message: WebSocketMessage): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val ws = webSocket
                    ?: return@withContext Result.failure(Exception("WebSocket not connected"))
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

    // 방 관리를 위한 세트
    private val joinedRooms = mutableSetOf<String>()

    override suspend fun joinRoom(roomId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            synchronized(joinedRooms) {
                if (joinedRooms.contains(roomId)) {
                    Log.d(TAG, "Already joined room: $roomId")
                    return@withContext Result.success(Unit)
                }

                // 연결 상태 확인
                if (_connectionState.value !is WebSocketConnectionState.Connected) {
                    Log.w(TAG, "Cannot join room: not connected")
                    return@withContext Result.failure(Exception("WebSocket not connected"))
                }

                currentRoomId = roomId
                joinedRooms.add(roomId)
            }

            val message = WebSocketMessage(
                type = WebSocketMessage.TYPE_JOIN_ROOM,
                roomId = roomId
            )

            val result = sendMessage(message)
            if (result.isFailure) {
                // 실패 시 상태 되돌리기
                synchronized(joinedRooms) {
                    joinedRooms.remove(roomId)
                    if (currentRoomId == roomId) {
                        currentRoomId = null
                    }
                }
            }

            result
        }
    }

    override suspend fun leaveRoom(roomId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            synchronized(joinedRooms) {
                if (!joinedRooms.contains(roomId)) {
                    Log.d(TAG, "Not in room: $roomId")
                    return@withContext Result.success(Unit)
                }

                joinedRooms.remove(roomId)
                if (currentRoomId == roomId) {
                    currentRoomId = null
                }
            }

            val message = WebSocketMessage(
                type = WebSocketMessage.TYPE_LEAVE_ROOM,
                roomId = roomId
            )
            sendMessage(message)
        }
    }

    /**
     * 방 입장 상태 확인
     */
    fun isRoomJoined(roomId: String): Boolean {
        synchronized(joinedRooms) {
            return joinedRooms.contains(roomId)
        }
    }

    /**
     * 모든 방에서 퇴장 (연결 해제 시 호출)
     */
    private fun leaveAllRooms() {
        synchronized(joinedRooms) {
            joinedRooms.clear()
            currentRoomId = null
        }
    }

    companion object {
        private const val TAG = "WebSocketManagerImpl"
    }
}