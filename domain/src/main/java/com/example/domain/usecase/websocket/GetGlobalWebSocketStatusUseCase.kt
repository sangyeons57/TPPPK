package com.example.domain.usecase.websocket

import com.example.core_common.websocket.GlobalWebSocketService
import com.example.core_common.websocket.WebSocketConnectionState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 글로벌 WebSocket 연결 상태를 관찰하는 UseCase
 * 
 * 앱 전체의 WebSocket 연결 상태를 실시간으로 모니터링할 수 있습니다.
 */
class GetGlobalWebSocketStatusUseCase @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService
) {
    
    /**
     * 글로벌 WebSocket 연결 상태를 반환합니다.
     * 
     * @return 연결 상태 StateFlow
     */
    operator fun invoke(): StateFlow<WebSocketConnectionState> {
        return globalWebSocketService.globalConnectionState
    }
    
    /**
     * 앱이 포그라운드에 있는지 상태를 반환합니다.
     * 
     * @return 포그라운드 상태 StateFlow
     */
    fun getForegroundState(): StateFlow<Boolean> {
        return globalWebSocketService.isInForeground
    }
}