package com.example.websocket.usecase

import com.example.websocket.core.WebSocketConnectionState
import com.example.websocket.service.GlobalWebSocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global WebSocket 상태 조회를 위한 UseCase
 *
 * 앱 전체의 WebSocket 연결 상태와 관련 정보를 조회하는 기능을 제공한다.
 * UI에서 연결 상태를 표시하거나 연결 상태에 따른 로직 처리에 사용된다.
 */
@Singleton
class GetGlobalWebSocketStatusUseCase @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService
) {

    /**
     * 현재 WebSocket 연결 상태를 실시간으로 관찰
     *
     * @return WebSocketConnectionState의 Flow
     * - Connected: 연결됨
     * - Connecting: 연결 중
     * - Disconnected: 연결 해제됨
     * - Error: 연결 오류
     */
    fun getConnectionState(): StateFlow<WebSocketConnectionState> {
        return globalWebSocketService.globalConnectionState
    }

    /**
     * 앱이 현재 포그라운드 상태인지 확인
     *
     * @return Boolean의 Flow (true: 포그라운드, false: 백그라운드)
     */
    fun getAppForegroundState(): StateFlow<Boolean> {
        return globalWebSocketService.isInForeground
    }

    /**
     * WebSocket 인증 상태를 실시간으로 관찰
     *
     * @return Boolean의 Flow (true: 인증됨, false: 인증되지 않음)
     */
    fun getAuthenticationState(): Flow<Boolean> {
        return globalWebSocketService.getWebSocketManager().isAuthenticated
    }

    /**
     * 현재 WebSocket 연결이 활성 상태인지 확인
     *
     * @return 연결되고 인증된 상태면 true, 그렇지 않으면 false
     */
    fun isWebSocketActive(): Boolean {
        val connectionState = globalWebSocketService.globalConnectionState.value
        val isAuthenticated = globalWebSocketService.getWebSocketManager().isAuthenticated.value

        return connectionState is WebSocketConnectionState.Connected && isAuthenticated
    }

    /**
     * 현재 연결 상태의 스냅샷 반환
     *
     * @return 현재 시점의 WebSocketConnectionState
     */
    fun getCurrentConnectionState(): WebSocketConnectionState {
        return globalWebSocketService.globalConnectionState.value
    }

    /**
     * 현재 인증 상태의 스냅샷 반환
     *
     * @return 현재 시점의 인증 상태 (true/false)
     */
    fun getCurrentAuthenticationState(): Boolean {
        return globalWebSocketService.getWebSocketManager().isAuthenticated.value
    }

    /**
     * 현재 앱 포그라운드 상태의 스냅샷 반환
     *
     * @return 현재 시점의 포그라운드 상태 (true/false)
     */
    fun getCurrentAppForegroundState(): Boolean {
        return globalWebSocketService.isInForeground.value
    }
}