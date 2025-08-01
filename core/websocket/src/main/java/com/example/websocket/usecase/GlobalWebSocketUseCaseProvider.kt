package com.example.websocket.usecase

import com.example.websocket.service.GlobalWebSocketService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global WebSocket 관련 UseCase들을 제공하는 Provider
 *
 * 앱 전체의 WebSocket 연결 상태 및 제어 기능을 관리하는 Use Case들을 제공한다.
 */
@Singleton
class GlobalWebSocketUseCaseProvider @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService
) {

    /**
     * Global WebSocket 사용 사례들을 제공
     */
    fun create(): GlobalWebSocketUseCases {
        return GlobalWebSocketUseCases(globalWebSocketService)
    }
}

/**
 * Global WebSocket 사용 사례들
 */
class GlobalWebSocketUseCases(
    private val globalWebSocketService: GlobalWebSocketService
) {

    // ================================
    // 서비스 생명주기 Use Cases
    // ================================

    /**
     * WebSocket 서비스 시작
     */
    fun startServiceUseCase() {
        globalWebSocketService.startService()
    }

    /**
     * WebSocket 서비스 중지
     */
    fun stopServiceUseCase() {
        globalWebSocketService.stopService()
    }

    /**
     * WebSocket 서버 URL 설정
     */
    fun configureUseCase(serverUrl: String) {
        globalWebSocketService.configure(serverUrl)
    }

    // ================================
    // 연결 제어 Use Cases
    // ================================

    /**
     * 강제 재연결
     */
    fun forceReconnectUseCase() {
        globalWebSocketService.forceReconnect()
    }

    /**
     * 강제 연결 해제
     */
    fun forceDisconnectUseCase() {
        globalWebSocketService.forceDisconnect()
    }

    /**
     * 재연결 상태 초기화
     */
    fun resetReconnectionStateUseCase() {
        globalWebSocketService.resetReconnectionState()
    }

    // ================================
    // 앱 생명주기 Use Cases
    // ================================

    /**
     * 앱이 포그라운드로 진입했을 때
     */
    fun onAppForegroundedUseCase() {
        globalWebSocketService.onAppForegrounded()
    }

    /**
     * 앱이 백그라운드로 진입했을 때
     */
    fun onAppBackgroundedUseCase() {
        globalWebSocketService.onAppBackgrounded()
    }
}