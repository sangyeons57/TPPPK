package com.example.feature_chat.websocket

import com.example.core_common.websocket.WebSocketManager
import com.example.core_common.websocket.WebSocketMessage
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.feature_chat.logging.ChatLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketClient @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val chatLogger: ChatLogger
) {
    
    val connectionState = webSocketManager.connectionState
    
    fun getChatMessages(roomId: String): Flow<ChatWebSocketEvent> {
        chatLogger.logDebug(
            ChatLogger.CATEGORY_WEBSOCKET,
            "getChatMessages 시작",
            mapOf("roomId" to roomId)
        )
        
        return webSocketManager.incomingMessages
            .filter { it.roomId == roomId }
            .onEach { message ->
                chatLogger.logWebSocketMessage(
                    action = "RECEIVE",
                    messageId = message.messageId ?: "unknown",
                    roomId = roomId,
                    userId = message.senderId,
                    success = true
                )
            }
            .map { message ->
                when (message.type) {
                    WebSocketMessage.TYPE_MESSAGE -> {
                        ChatWebSocketEvent.MessageReceived(
                            messageId = message.messageId ?: "",
                            senderId = message.senderId ?: "",
                            content = message.content ?: "",
                            timestamp = message.timestamp ?: Instant.now().toString(),
                            replyToMessageId = message.replyToMessageId
                        )
                    }
                    WebSocketMessage.TYPE_EDIT_MESSAGE -> {
                        ChatWebSocketEvent.MessageEdited(
                            messageId = message.messageId ?: "",
                            newContent = message.content ?: "",
                            timestamp = message.timestamp ?: Instant.now().toString()
                        )
                    }
                    WebSocketMessage.TYPE_DELETE_MESSAGE -> {
                        ChatWebSocketEvent.MessageDeleted(
                            messageId = message.messageId ?: "",
                            timestamp = message.timestamp ?: Instant.now().toString()
                        )
                    }
                    WebSocketMessage.TYPE_SYSTEM -> {
                        ChatWebSocketEvent.SystemMessage(
                            content = message.content ?: "",
                            timestamp = message.timestamp ?: Instant.now().toString()
                        )
                    }
                    WebSocketMessage.TYPE_ERROR -> {
                        chatLogger.logError(
                            ChatLogger.CATEGORY_WEBSOCKET,
                            "WebSocket 에러 수신: ${message.content}",
                            metadata = mapOf("roomId" to roomId)
                        )
                        ChatWebSocketEvent.Error(
                            message = message.content ?: "Unknown error"
                        )
                    }
                    else -> {
                        chatLogger.logWarning(
                            ChatLogger.CATEGORY_WEBSOCKET,
                            "알 수 없는 메시지 타입: ${message.type}",
                            metadata = mapOf("roomId" to roomId, "messageType" to message.type)
                        )
                        ChatWebSocketEvent.Unknown(message.type)
                    }
                }
            }
    }
    
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        chatLogger.logInfo(
            ChatLogger.CATEGORY_CONNECTION,
            "WebSocket 연결 시도",
            mapOf("serverUrl" to serverUrl)
        )
        
        return webSocketManager.connect(serverUrl, authToken).also { result ->
            chatLogger.logWebSocketConnection(
                success = result.isSuccess,
                serverUrl = serverUrl,
                userId = null // userId는 인증 후에 알 수 있음
            )
            
            if (result.isFailure) {
                chatLogger.logError(
                    ChatLogger.CATEGORY_CONNECTION,
                    "WebSocket 연결 실패",
                    result.exceptionOrNull(),
                    mapOf("serverUrl" to serverUrl)
                )
            }
        }
    }
    
    suspend fun disconnect() {
        chatLogger.logInfo(ChatLogger.CATEGORY_CONNECTION, "WebSocket 연결 해제 시도")
        webSocketManager.disconnect()
        chatLogger.logInfo(ChatLogger.CATEGORY_CONNECTION, "WebSocket 연결 해제 완료")
    }
    
    suspend fun joinRoom(roomId: String): Result<Unit> {
        chatLogger.logInfo(
            ChatLogger.CATEGORY_CONNECTION,
            "채팅방 입장 시도",
            roomId = roomId
        )
        
        return webSocketManager.joinRoom(roomId).also { result ->
            if (result.isSuccess) {
                chatLogger.logInfo(
                    ChatLogger.CATEGORY_CONNECTION,
                    "채팅방 입장 성공",
                    roomId = roomId
                )
            } else {
                chatLogger.logError(
                    ChatLogger.CATEGORY_CONNECTION,
                    "채팅방 입장 실패",
                    result.exceptionOrNull(),
                    roomId = roomId
                )
            }
        }
    }
    
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        chatLogger.logInfo(
            ChatLogger.CATEGORY_CONNECTION,
            "채팅방 퇴장 시도",
            roomId = roomId
        )
        
        return webSocketManager.leaveRoom(roomId).also { result ->
            if (result.isSuccess) {
                chatLogger.logInfo(
                    ChatLogger.CATEGORY_CONNECTION,
                    "채팅방 퇴장 성공",
                    roomId = roomId
                )
            } else {
                chatLogger.logError(
                    ChatLogger.CATEGORY_CONNECTION,
                    "채팅방 퇴장 실패",
                    result.exceptionOrNull(),
                    roomId = roomId
                )
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
        chatLogger.logInfo(
            ChatLogger.CATEGORY_MESSAGE,
            "메시지 전송 시도",
            mapOf("contentLength" to content.length.toString()),
            userId = senderId.value,
            roomId = roomId,
            messageId = messageId.value
        )
        
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_MESSAGE,
            roomId = roomId,
            senderId = senderId.value,
            content = content,
            messageId = messageId.value,
            replyToMessageId = replyToMessageId?.value,
            timestamp = Instant.now().toString()
        )
        
        return webSocketManager.sendMessage(message).also { result ->
            chatLogger.logWebSocketMessage(
                action = "SEND",
                messageId = messageId.value,
                roomId = roomId,
                userId = senderId.value,
                success = result.isSuccess
            )
            
            if (result.isFailure) {
                chatLogger.logError(
                    ChatLogger.CATEGORY_MESSAGE,
                    "메시지 전송 실패",
                    result.exceptionOrNull(),
                    userId = senderId.value,
                    roomId = roomId,
                    messageId = messageId.value
                )
            }
        }
    }
    
    suspend fun editMessage(
        roomId: String,
        messageId: DocumentId,
        newContent: String
    ): Result<Unit> {
        chatLogger.logInfo(
            ChatLogger.CATEGORY_MESSAGE,
            "메시지 수정 시도",
            mapOf("newContentLength" to newContent.length.toString()),
            roomId = roomId,
            messageId = messageId.value
        )
        
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_EDIT_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            content = newContent,
            timestamp = Instant.now().toString()
        )
        
        return webSocketManager.sendMessage(message).also { result ->
            chatLogger.logWebSocketMessage(
                action = "EDIT",
                messageId = messageId.value,
                roomId = roomId,
                userId = null, // edit에서는 senderId가 없음
                success = result.isSuccess
            )
            
            if (result.isFailure) {
                chatLogger.logError(
                    ChatLogger.CATEGORY_MESSAGE,
                    "메시지 수정 실패",
                    result.exceptionOrNull(),
                    roomId = roomId,
                    messageId = messageId.value
                )
            }
        }
    }
    
    suspend fun deleteMessage(
        roomId: String,
        messageId: DocumentId
    ): Result<Unit> {
        chatLogger.logInfo(
            ChatLogger.CATEGORY_MESSAGE,
            "메시지 삭제 시도",
            roomId = roomId,
            messageId = messageId.value
        )
        
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_DELETE_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            timestamp = Instant.now().toString()
        )
        
        return webSocketManager.sendMessage(message).also { result ->
            chatLogger.logWebSocketMessage(
                action = "DELETE",
                messageId = messageId.value,
                roomId = roomId,
                userId = null, // delete에서는 senderId가 없음
                success = result.isSuccess
            )
            
            if (result.isFailure) {
                chatLogger.logError(
                    ChatLogger.CATEGORY_MESSAGE,
                    "메시지 삭제 실패",
                    result.exceptionOrNull(),
                    roomId = roomId,
                    messageId = messageId.value
                )
            }
        }
    }
}