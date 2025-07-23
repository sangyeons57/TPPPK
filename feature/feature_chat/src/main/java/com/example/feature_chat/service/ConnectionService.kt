package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * WebSocket 연결 관리를 담당하는 Service
 * 연결 상태 모니터링, 연결 재시도, 상태 텍스트 제공 등의 기능을 제공합니다.
 */
class ConnectionService(
    private val webSocketClient: ChatWebSocketClient,
    private val offlineMessageQueue: OfflineMessageQueue
) {
    
    data class ConnectionInfo(
        val state: WebSocketConnectionState,
        val queuedMessagesCount: Int,
        val showConnectionError: Boolean,
        val statusText: String
    )
    
    /**
     * 연결 상태를 스트림으로 제공
     */
    fun getConnectionStateStream(): Flow<ConnectionInfo> {
        return webSocketClient.connectionState.map { state ->
            val queuedCount = offlineMessageQueue.getQueueSize()
            val showError = state is WebSocketConnectionState.Error
            val statusText = getConnectionStatusText(state, queuedCount)
            
            Log.d("ConnectionService", "Connection state changed: $state, queued: $queuedCount")
            
            ConnectionInfo(
                state = state,
                queuedMessagesCount = queuedCount,
                showConnectionError = showError,
                statusText = statusText
            )
        }
    }
    
    /**
     * 특정 채팅방의 메시지 이벤트를 스트림으로 제공
     */
    fun getChatMessageEvents(roomId: String): Flow<ChatWebSocketEvent> {
        return webSocketClient.getChatMessages(roomId)
    }
    
    /**
     * 연결 재시도
     */
    suspend fun retryConnection(): Result<Unit> {
        return try {
            Log.d("ConnectionService", "Manual retry connection requested...")
            
            // Reset manual disconnect flag and force reconnection
            val webSocketManager = webSocketClient.webSocketManager
            if (webSocketManager is com.example.core_common.websocket.WebSocketManagerImpl) {
                // Reset reconnection state for manual retry
                webSocketManager.resetReconnectionState()
            }
            
            // Trigger reconnection through GlobalWebSocketService
            webSocketClient.globalWebSocketService.forceReconnect()
            
            Log.d("ConnectionService", "Manual retry connection initiated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ConnectionService", "Failed to retry connection", e)
            Result.failure(e)
        }
    }
    
    /**
     * 연결 상태를 기반으로 쓰기 작업 가능 여부 확인
     */
    fun canPerformWriteOperations(): Boolean {
        return webSocketClient.connectionState.value is WebSocketConnectionState.Connected
    }
    
    /**
     * 읽기 전용 모드인지 확인
     */
    fun isReadOnlyMode(): Boolean {
        return !canPerformWriteOperations()
    }
    
    /**
     * 연결 상태에 따른 상태 텍스트 생성
     */
    private fun getConnectionStatusText(
        state: WebSocketConnectionState,
        queuedCount: Int
    ): String {
        return when (state) {
            is WebSocketConnectionState.Connected -> "실시간 연결됨"
            is WebSocketConnectionState.Connecting -> "연결 중..."
            is WebSocketConnectionState.Disconnected -> {
                if (queuedCount > 0) {
                    "오프라인 (${queuedCount}개 대기중)"
                } else {
                    "오프라인 (읽기 전용)"
                }
            }
            is WebSocketConnectionState.Error -> "연결 오류: ${state.message}"
        }
    }
    
    /**
     * 현재 연결 상태 반환
     */
    fun getCurrentConnectionState(): WebSocketConnectionState {
        return webSocketClient.connectionState.value
    }
    
    /**
     * 오프라인 메시지 큐 크기 반환
     */
    fun getQueuedMessagesCount(): Int {
        return offlineMessageQueue.getQueueSize()
    }
    
    /**
     * 연결 오류 상태인지 확인
     */
    fun hasConnectionError(): Boolean {
        return webSocketClient.connectionState.value is WebSocketConnectionState.Error
    }
}