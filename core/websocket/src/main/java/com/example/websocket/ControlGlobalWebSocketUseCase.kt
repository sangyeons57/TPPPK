package com.example.websocket

import javax.inject.Inject

/**
 * 글로벌 WebSocket 연결을 제어하는 UseCase
 *
 * 글로벌 WebSocket 서비스의 연결, 재연결, 해제 등을 제어할 수 있습니다.
 */
class ControlGlobalWebSocketUseCase @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService
) {

    /**
     * WebSocket 강제 재연결을 수행합니다.
     *
     * 연결 문제가 발생했을 때 수동으로 재연결을 시도할 수 있습니다.
     */
    fun forceReconnect() {
        globalWebSocketService.forceReconnect()
    }

    /**
     * WebSocket 연결을 강제로 해제합니다.
     *
     * 특정 상황에서 연결을 수동으로 해제할 수 있습니다.
     */
    fun forceDisconnect() {
        globalWebSocketService.forceDisconnect()
    }

    /**
     * 기본 WebSocketManager에 접근합니다.
     *
     * 메시지 전송 등의 작업을 위해 기본 WebSocketManager를 반환합니다.
     *
     * @return WebSocketManager 인스턴스
     */
    fun getWebSocketManager(): WebSocketManager {
        return globalWebSocketService.getWebSocketManager()
    }
}