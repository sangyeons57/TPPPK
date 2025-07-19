package com.example.core_common.websocket

import kotlinx.coroutines.flow.StateFlow

interface WebSocketManager {
    val connectionState: StateFlow<WebSocketConnectionState>
    val incomingMessages: kotlinx.coroutines.flow.Flow<WebSocketMessage>
    
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit>
    suspend fun disconnect()
    suspend fun sendMessage(message: WebSocketMessage): Result<Unit>
    suspend fun joinRoom(roomId: String): Result<Unit>
    suspend fun leaveRoom(roomId: String): Result<Unit>
}