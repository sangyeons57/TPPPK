package com.example.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketManager {
    companion object {
        const val SERVER_URL = "wss://websocket-chat-wizwlraydq-du.a.run.app/chat"
    }

    val connectionState: StateFlow<WebSocketConnectionState>
    val incomingMessages: Flow<WebSocketMessage>
    val isAuthenticated: StateFlow<Boolean>
    
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit>
    suspend fun disconnect()
    suspend fun sendMessage(message: WebSocketMessage): Result<Unit>
    suspend fun joinRoom(roomId: String): Result<Unit>
    suspend fun leaveRoom(roomId: String): Result<Unit>
}