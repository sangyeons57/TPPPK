package com.example.websocket.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * WebSocket 연결 및 메시지 관리를 위한 인터페이스
 *
 * WebSocket의 핵심 기능을 정의하며, 실제 구현은 WebSocketManagerImpl에서 담당한다.
 * 이 인터페이스는 테스트 가능성과 확장성을 위해 추상화되어 있다.
 */
interface WebSocketManager {

    companion object {
        const val SERVER_URL = "wss://websocket-chat-wizwlraydq-du.a.run.app/chat"
    }

    // ================================
    // 상태 관리
    // ================================

    /**
     * 현재 WebSocket 연결 상태
     */
    val connectionState: StateFlow<WebSocketConnectionState>

    /**
     * 인증 상태 (서버로부터 AUTH_SUCCESS를 받았는지 여부)
     */
    val isAuthenticated: StateFlow<Boolean>

    /**
     * 수신된 메시지 스트림
     */
    val incomingMessages: Flow<WebSocketMessage>

    // ================================
    // 연결 관리
    // ================================

    /**
     * WebSocket 서버에 연결
     *
     * @param serverUrl WebSocket 서버 URL
     * @param authToken 인증 토큰
     * @return 연결 성공 여부
     */
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit>

    /**
     * WebSocket 연결 해제
     *
     * @return 연결 해제 성공 여부
     */
    suspend fun disconnect(): Result<Unit>

    // ================================
    // 메시지 전송
    // ================================

    /**
     * WebSocket을 통해 메시지 전송
     *
     * @param message 전송할 메시지
     * @return 전송 성공 여부
     */
    suspend fun sendMessage(message: WebSocketMessage): Result<Unit>

    // ================================
    // 방 관리
    // ================================

    /**
     * 특정 방에 입장
     *
     * @param roomId 방 ID
     * @return 입장 성공 여부
     */
    suspend fun joinRoom(roomId: String): Result<Unit>

    /**
     * 특정 방에서 퇴장
     *
     * @param roomId 방 ID
     * @return 퇴장 성공 여부
     */
    suspend fun leaveRoom(roomId: String): Result<Unit>

    // ================================
    // 인증 관리
    // ================================

    /**
     * 서버에 인증 메시지 전송
     *
     * @param authToken 인증 토큰
     * @return 인증 메시지 전송 성공 여부
     */
    suspend fun authenticate(authToken: String): Result<Unit>
}