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
     * 방에 입장합니다.
     *
     * @param roomId 입장할 방 ID
     * @return 입장 결과
     */
    suspend fun joinRoom(roomId: String): Result<Unit> {
        return globalWebSocketService.joinRoom(roomId)
    }

    /**
     * 방에서 퇴장합니다.
     *
     * @param roomId 퇴장할 방 ID
     * @return 퇴장 결과
     */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return globalWebSocketService.leaveRoom(roomId)
    }

    /**
     * 현재 입장한 방 목록을 가져옵니다.
     *
     * @return 입장한 방 ID 집합
     */
    fun getJoinedRooms(): Set<String> {
        return globalWebSocketService.getJoinedRooms()
    }

    /**
     * 특정 방에 입장했는지 확인합니다.
     *
     * @param roomId 확인할 방 ID
     * @return 입장 여부
     */
    fun isJoinedToRoom(roomId: String): Boolean {
        return globalWebSocketService.isJoinedToRoom(roomId)
    }

    /**
     * WebSocket 연결 상태를 가져옵니다.
     */
    fun getConnectionState() = globalWebSocketService.globalConnectionState

    /**
     * WebSocket 메시지 스트림을 가져옵니다.
     */
    fun getIncomingMessages() = globalWebSocketService.getWebSocketManager().incomingMessages

    /**
     * WebSocket 메시지를 전송합니다.
     *
     * @param message 전송할 메시지
     * @return 전송 결과
     */
    suspend fun sendMessage(message: WebSocketMessage): Result<Unit> {
        return globalWebSocketService.getWebSocketManager().sendMessage(message)
    }
}