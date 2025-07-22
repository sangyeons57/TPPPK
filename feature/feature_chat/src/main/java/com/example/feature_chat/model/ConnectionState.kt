package com.example.feature_chat.model

import com.example.core_common.websocket.WebSocketConnectionState

/**
 * WebSocket 연결 상태와 관련된 UI 상태를 관리하는 데이터 클래스
 */
data class ConnectionState(
    val connectionState: WebSocketConnectionState = WebSocketConnectionState.Disconnected,
    val queuedMessagesCount: Int = 0,
    val showConnectionError: Boolean = false,
    val statusText: String = "연결 중...",
    val lastConnectionTime: Long? = null,
    val retryCount: Int = 0,
    val isRetrying: Boolean = false
) {
    
    /**
     * 연결되어 있는지 확인
     */
    fun isConnected(): Boolean {
        return connectionState is WebSocketConnectionState.Connected
    }
    
    /**
     * 연결 중인지 확인
     */
    fun isConnecting(): Boolean {
        return connectionState is WebSocketConnectionState.Connecting
    }
    
    /**
     * 연결이 끊어졌는지 확인
     */
    fun isDisconnected(): Boolean {
        return connectionState is WebSocketConnectionState.Disconnected
    }
    
    /**
     * 오류 상태인지 확인
     */
    fun hasError(): Boolean {
        return connectionState is WebSocketConnectionState.Error
    }
    
    /**
     * 쓰기 작업이 가능한지 확인
     */
    fun canPerformWriteOperations(): Boolean {
        return isConnected()
    }
    
    /**
     * 읽기 전용 모드인지 확인
     */
    fun isReadOnlyMode(): Boolean {
        return !canPerformWriteOperations()
    }
    
    /**
     * 대기 중인 메시지가 있는지 확인
     */
    fun hasQueuedMessages(): Boolean {
        return queuedMessagesCount > 0
    }
    
    /**
     * 연결 오류 메시지 반환
     */
    fun getErrorMessage(): String? {
        return when (val state = connectionState) {
            is WebSocketConnectionState.Error -> state.message
            else -> null
        }
    }
    
    companion object {
        /**
         * 초기 상태 생성
         */
        fun initial(): ConnectionState {
            return ConnectionState()
        }
        
        /**
         * 연결된 상태 생성
         */
        fun connected(): ConnectionState {
            return ConnectionState(
                connectionState = WebSocketConnectionState.Connected,
                statusText = "실시간 연결됨",
                showConnectionError = false,
                lastConnectionTime = System.currentTimeMillis(),
                retryCount = 0,
                isRetrying = false
            )
        }
        
        /**
         * 연결 중 상태 생성
         */
        fun connecting(retryCount: Int = 0): ConnectionState {
            return ConnectionState(
                connectionState = WebSocketConnectionState.Connecting,
                statusText = if (retryCount > 0) "재연결 시도 중... ($retryCount)" else "연결 중...",
                retryCount = retryCount,
                isRetrying = retryCount > 0
            )
        }
        
        /**
         * 오프라인 상태 생성
         */
        fun offline(queuedCount: Int = 0): ConnectionState {
            val statusText = if (queuedCount > 0) {
                "오프라인 (${queuedCount}개 대기중)"
            } else {
                "오프라인 (읽기 전용)"
            }
            
            return ConnectionState(
                connectionState = WebSocketConnectionState.Disconnected,
                queuedMessagesCount = queuedCount,
                statusText = statusText
            )
        }
        
        /**
         * 오류 상태 생성
         */
        fun error(message: String, queuedCount: Int = 0): ConnectionState {
            return ConnectionState(
                connectionState = WebSocketConnectionState.Error(message),
                queuedMessagesCount = queuedCount,
                showConnectionError = true,
                statusText = "연결 오류: $message"
            )
        }
    }
}