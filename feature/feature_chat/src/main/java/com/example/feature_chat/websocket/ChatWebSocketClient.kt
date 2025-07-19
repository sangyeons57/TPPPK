package com.example.feature_chat.websocket

import com.example.core_common.websocket.WebSocketManager
import com.example.core_common.websocket.WebSocketMessage
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketClient @Inject constructor(
    private val webSocketManager: WebSocketManager
) {
    
    val connectionState = webSocketManager.connectionState
    
    fun getChatMessages(roomId: String): Flow<ChatWebSocketEvent> {
        return webSocketManager.incomingMessages
            .filter { it.roomId == roomId }
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
                        ChatWebSocketEvent.Error(
                            message = message.content ?: "Unknown error"
                        )
                    }
                    else -> ChatWebSocketEvent.Unknown(message.type)
                }
            }
    }
    
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        return webSocketManager.connect(serverUrl, authToken)
    }
    
    suspend fun disconnect() {
        webSocketManager.disconnect()
    }
    
    suspend fun joinRoom(roomId: String): Result<Unit> {
        return webSocketManager.joinRoom(roomId)
    }
    
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return webSocketManager.leaveRoom(roomId)
    }
    
    suspend fun sendMessage(
        roomId: String,
        senderId: UserId,
        content: String,
        messageId: DocumentId,
        replyToMessageId: DocumentId? = null
    ): Result<Unit> {
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_MESSAGE,
            roomId = roomId,
            senderId = senderId.value,
            content = content,
            messageId = messageId.value,
            replyToMessageId = replyToMessageId?.value,
            timestamp = Instant.now().toString()
        )
        return webSocketManager.sendMessage(message)
    }
    
    suspend fun editMessage(
        roomId: String,
        messageId: DocumentId,
        newContent: String
    ): Result<Unit> {
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_EDIT_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            content = newContent,
            timestamp = Instant.now().toString()
        )
        return webSocketManager.sendMessage(message)
    }
    
    suspend fun deleteMessage(
        roomId: String,
        messageId: DocumentId
    ): Result<Unit> {
        val message = WebSocketMessage(
            type = WebSocketMessage.TYPE_DELETE_MESSAGE,
            roomId = roomId,
            messageId = messageId.value,
            timestamp = Instant.now().toString()
        )
        return webSocketManager.sendMessage(message)
    }
}