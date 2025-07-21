package com.example.feature_chat.websocket

import com.example.core_common.websocket.GlobalWebSocketService
import com.example.core_common.websocket.WebSocketManager
import com.example.core_common.websocket.WebSocketMessage
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import android.util.Log
import com.example.feature_chat.utils.ChatLogUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketClient @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService
) {
    
    // Delegate to global service for connection state
    val connectionState = globalWebSocketService.globalConnectionState
    
    // Get the underlying WebSocketManager for direct operations
    private val webSocketManager: WebSocketManager = globalWebSocketService.getWebSocketManager()
    val isAuthenticated = webSocketManager.isAuthenticated
    
    fun getChatMessages(roomId: String): Flow<ChatWebSocketEvent> {
        val correlationId = ChatLogUtils.generateCorrelationId()
        Log.d(ChatLogUtils.TAG_WEBSOCKET, ChatLogUtils.formatLogMessage(
            correlationId = correlationId,
            message = "getChatMessages 시작",
            roomId = roomId
        ))
        
        return webSocketManager.incomingMessages
            .filter { it.roomId == roomId && it.type != WebSocketMessage.TYPE_AUTH_SUCCESS }
            .onEach { message ->
                val correlationId = ChatLogUtils.generateCorrelationId()
                Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                    correlationId = correlationId,
                    message = "WebSocket 메시지 RECEIVE 성공",
                    userId = message.senderId,
                    roomId = roomId,
                    messageId = message.messageId ?: "unknown",
                    metadata = mapOf("action" to "RECEIVE", "status" to "SUCCESS")
                ))
            }
            .map { message ->
                when (message.type) {
                    WebSocketMessage.TYPE_MESSAGE -> {
                        ChatWebSocketEvent.MessageReceived(
                            messageId = message.messageId ?: "",
                            senderId = message.senderId ?: "",
                            content = message.content ?: "",
                            timestamp = message.timestamp?.let { Instant.ofEpochSecond(it.toLong()).toString() } ?: Instant.now().toString(),
                            replyToMessageId = message.replyToMessageId
                        )
                    }
                    WebSocketMessage.TYPE_EDIT_MESSAGE -> {
                        ChatWebSocketEvent.MessageEdited(
                            messageId = message.messageId ?: "",
                            newContent = message.content ?: "",
                            timestamp = message.timestamp?.let { Instant.ofEpochSecond(it.toLong()).toString() } ?: Instant.now().toString()
                        )
                    }
                    WebSocketMessage.TYPE_DELETE_MESSAGE -> {
                        ChatWebSocketEvent.MessageDeleted(
                            messageId = message.messageId ?: "",
                            timestamp = message.timestamp?.let { Instant.ofEpochSecond(it.toLong()).toString() } ?: Instant.now().toString()
                        )
                    }
                    WebSocketMessage.TYPE_SYSTEM -> {
                        ChatWebSocketEvent.SystemMessage(
                            content = message.content ?: "",
                            timestamp = message.timestamp?.let { Instant.ofEpochSecond(it.toLong()).toString() } ?: Instant.now().toString()
                        )
                    }
                    WebSocketMessage.TYPE_ERROR -> {
                        val correlationId = ChatLogUtils.generateCorrelationId()
                        Log.e(ChatLogUtils.TAG_WEBSOCKET, ChatLogUtils.formatLogMessage(
                            correlationId = correlationId,
                            message = "WebSocket 에러 수신: ${message.content}",
                            roomId = roomId
                        ))
                        ChatWebSocketEvent.Error(
                            message = message.content ?: "Unknown error"
                        )
                    }
                    else -> {
                        val correlationId = ChatLogUtils.generateCorrelationId()
                        Log.w(ChatLogUtils.TAG_WEBSOCKET, ChatLogUtils.formatLogMessage(
                            correlationId = correlationId,
                            message = "알 수 없는 메시지 타입: ${message.type}",
                            roomId = roomId,
                            metadata = mapOf("messageType" to message.type)
                        ))
                        ChatWebSocketEvent.Unknown(message.type)
                    }
                }
            }
    }
    
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        val correlationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = correlationId,
            message = "WebSocket 연결 시도 (GlobalWebSocketService 사용)",
            metadata = mapOf("serverUrl" to serverUrl)
        ))
        
        // Note: Connection is now managed by GlobalWebSocketService
        // This method exists for compatibility but the actual connection
        // should already be established by the global service
        return try {
            // Configure the global service if needed
            globalWebSocketService.configure(serverUrl)
            
            // Force reconnect if not already connected
            if (connectionState.value !is com.example.core_common.websocket.WebSocketConnectionState.Connected) {
                globalWebSocketService.forceReconnect()
            }
            
            val result = Result.success(Unit)
            
            val connectionCorrelationId = ChatLogUtils.generateCorrelationId()
            Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                correlationId = connectionCorrelationId,
                message = "GlobalWebSocketService 연결 위임 완료",
                metadata = mapOf("serverUrl" to serverUrl, "status" to "DELEGATED")
            ))
            
            result
        } catch (e: Exception) {
            val errorCorrelationId = ChatLogUtils.generateCorrelationId()
            Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                correlationId = errorCorrelationId,
                message = "GlobalWebSocketService 연결 위임 실패: ${e.message}",
                metadata = mapOf("serverUrl" to serverUrl)
            ))
            Result.failure(e)
        }
    }
    
    /**
     * UserSession을 받아서 토큰을 추출하여 WebSocket에 연결합니다.
     * 토큰이 유효하지 않은 경우 오류를 반환합니다.
     */
    suspend fun connectWithSession(serverUrl: String, userSession: UserSession): Result<Unit> {
        val correlationId = ChatLogUtils.generateCorrelationId()
        
        // 토큰 유효성 검증
        if (userSession.idToken == null) {
            val error = Exception("Invalid or expired token in user session")
            Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                correlationId = correlationId,
                message = "WebSocket 연결 실패: 유효하지 않은 토큰",
                userId = userSession.userId.value,
            ))
            return Result.failure(error)
        }
        
        val authToken = userSession.idToken!!.value
        
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = correlationId,
            message = "UserSession으로 WebSocket 연결 시도",
            userId = userSession.userId.value,
        ))
        
        return connect(serverUrl, authToken)
    }
    
    suspend fun waitForAuthentication(userId: UserId, timeoutMs: Long = 10000): Result<Unit> {
        val authCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = authCorrelationId,
            message = "WebSocket 인증 확인 대기",
            userId = userId.value
        ))
        
        // Wait for AUTH_SUCCESS message from server
        return try {
            val authResult = withTimeoutOrNull(timeoutMs) {
                webSocketManager.incomingMessages
                    .filter { message -> 
                        message.type == WebSocketMessage.TYPE_AUTH_SUCCESS || 
                        message.type == WebSocketMessage.TYPE_ERROR 
                    }
                    .first()
            }
            
            when {
                authResult == null -> {
                    Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                        correlationId = authCorrelationId,
                        message = "WebSocket 인증 시간 초과",
                        userId = userId.value
                    ))
                    Result.failure(Exception("Authentication timeout - no AUTH_SUCCESS received"))
                }
                authResult.type == WebSocketMessage.TYPE_AUTH_SUCCESS -> {
                    Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                        correlationId = authCorrelationId,
                        message = "WebSocket 인증 성공",
                        userId = userId.value
                    ))
                    Result.success(Unit)
                }
                authResult.type == WebSocketMessage.TYPE_ERROR -> {
                    Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                        correlationId = authCorrelationId,
                        message = "WebSocket 인증 실패: ${authResult.content}",
                        userId = userId.value
                    ))
                    Result.failure(Exception("Authentication failed: ${authResult.content}"))
                }
                else -> {
                    Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                        correlationId = authCorrelationId,
                        message = "WebSocket 인증 처리 중 예상치 못한 메시지 타입: ${authResult.type}",
                        userId = userId.value
                    ))
                    Result.failure(Exception("Unexpected message type during authentication: ${authResult.type}"))
                }
            }
        } catch (e: Exception) {
            Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                correlationId = authCorrelationId,
                message = "WebSocket 인증 대기 중 오류 발생: ${e.message}",
                userId = userId.value
            ))
            Result.failure(e)
        }
    }
    
    suspend fun disconnect() {
        val disconnectCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = disconnectCorrelationId,
            message = "WebSocket 연결 해제 시도 (GlobalWebSocketService는 유지)"
        ))
        
        // Note: We don't disconnect the global service as it's managed app-wide
        // Individual chat features should only leave their rooms
        // The global connection remains for other features to use
        
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = disconnectCorrelationId,
            message = "개별 채팅 기능 종료 - GlobalWebSocketService는 계속 활성 상태"
        ))
    }
    
    suspend fun joinRoom(roomId: String, userId: UserId? = null): Result<Unit> {
        val joinCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = joinCorrelationId,
            message = "채팅방 입장 시도",
            roomId = roomId,
            userId = userId?.value
        ))
        
        // userId가 제공된 경우 직접 메시지를 보내고, 그렇지 않으면 기본 WebSocketManager 사용
        val result = if (userId != null) {
            val joinMessage = WebSocketMessage(
                type = WebSocketMessage.TYPE_JOIN_ROOM,
                roomId = roomId,
                senderId = userId.value,
                timestamp = Instant.now().epochSecond.toDouble()
            )
            webSocketManager.sendMessage(joinMessage)
        } else {
            webSocketManager.joinRoom(roomId)
        }
        
        return result.also { result ->
            if (result.isSuccess) {
                Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                    correlationId = joinCorrelationId,
                    message = "채팅방 입장 성공",
                    roomId = roomId,
                    userId = userId?.value
                ))
            } else {
                Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                    correlationId = joinCorrelationId,
                    message = "채팅방 입장 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId,
                    userId = userId?.value
                ))
            }
        }
    }
    
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        val leaveCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
            correlationId = leaveCorrelationId,
            message = "채팅방 퇴장 시도",
            roomId = roomId
        ))
        
        return webSocketManager.leaveRoom(roomId).also { result ->
            if (result.isSuccess) {
                Log.i(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                    correlationId = leaveCorrelationId,
                    message = "채팅방 퇴장 성공",
                    roomId = roomId
                ))
            } else {
                Log.e(ChatLogUtils.TAG_CONNECTION, ChatLogUtils.formatLogMessage(
                    correlationId = leaveCorrelationId,
                    message = "채팅방 퇴장 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId
                ))
            }
        }
    }
    
    suspend fun sendMessage(
        roomId: String,
        senderId: UserId,
        content: String,
        messageId: DocumentId,
        replyToMessageId: DocumentId? = null
    ): Result<Unit> {
        val sendCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
            correlationId = sendCorrelationId,
            message = "메시지 전송 시도",
            userId = senderId.value,
            roomId = roomId,
            messageId = messageId.value,
            metadata = mapOf("contentLength" to content.length.toString())
        ))
        
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_MESSAGE,
            roomId = roomId,
            senderId = senderId.value,
            content = content,
            messageId = messageId.value,
            replyToMessageId = replyToMessageId?.value,
            timestamp = Instant.now().epochSecond.toDouble()
        )
        
        return webSocketManager.sendMessage(message).also { result ->
            val status = if (result.isSuccess) "SUCCESS" else "FAILED"
            Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                correlationId = sendCorrelationId,
                message = "WebSocket 메시지 SEND $status",
                userId = senderId.value,
                roomId = roomId,
                messageId = messageId.value,
                metadata = mapOf("action" to "SEND", "status" to status)
            ))
            
            if (result.isFailure) {
                Log.e(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                    correlationId = sendCorrelationId,
                    message = "메시지 전송 실패: ${result.exceptionOrNull()?.message}",
                    userId = senderId.value,
                    roomId = roomId,
                    messageId = messageId.value
                ))
            }
        }
    }
    
    suspend fun editMessage(
        roomId: String,
        messageId: DocumentId,
        newContent: String
    ): Result<Unit> {
        val editCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
            correlationId = editCorrelationId,
            message = "메시지 수정 시도",
            roomId = roomId,
            messageId = messageId.value,
            metadata = mapOf("newContentLength" to newContent.length.toString())
        ))
        
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_EDIT_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            content = newContent,
            timestamp = Instant.now().epochSecond.toDouble()
        )
        
        return webSocketManager.sendMessage(message).also { result ->
            val status = if (result.isSuccess) "SUCCESS" else "FAILED"
            Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                correlationId = editCorrelationId,
                message = "WebSocket 메시지 EDIT $status",
                roomId = roomId,
                messageId = messageId.value,
                metadata = mapOf("action" to "EDIT", "status" to status)
            ))
            
            if (result.isFailure) {
                Log.e(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                    correlationId = editCorrelationId,
                    message = "메시지 수정 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId,
                    messageId = messageId.value
                ))
            }
        }
    }
    
    suspend fun deleteMessage(
        roomId: String,
        messageId: DocumentId
    ): Result<Unit> {
        val deleteCorrelationId = ChatLogUtils.generateCorrelationId()
        Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
            correlationId = deleteCorrelationId,
            message = "메시지 삭제 시도",
            roomId = roomId,
            messageId = messageId.value
        ))
        
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_DELETE_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            timestamp = Instant.now().epochSecond.toDouble()
        )
        
        return webSocketManager.sendMessage(message).also { result ->
            val status = if (result.isSuccess) "SUCCESS" else "FAILED"
            Log.i(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                correlationId = deleteCorrelationId,
                message = "WebSocket 메시지 DELETE $status",
                roomId = roomId,
                messageId = messageId.value,
                metadata = mapOf("action" to "DELETE", "status" to status)
            ))
            
            if (result.isFailure) {
                Log.e(ChatLogUtils.TAG_MESSAGE, ChatLogUtils.formatLogMessage(
                    correlationId = deleteCorrelationId,
                    message = "메시지 삭제 실패: ${result.exceptionOrNull()?.message}",
                    roomId = roomId,
                    messageId = messageId.value
                ))
            }
        }
    }
}