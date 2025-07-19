package com.example.core_common.websocket

sealed class WebSocketConnectionState {
    data object Disconnected : WebSocketConnectionState()
    data object Connecting : WebSocketConnectionState()
    data object Connected : WebSocketConnectionState()
    data class Error(val message: String, val throwable: Throwable? = null) : WebSocketConnectionState()
}