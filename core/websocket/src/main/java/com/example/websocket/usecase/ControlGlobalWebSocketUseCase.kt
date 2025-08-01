package com.example.websocket.usecase

import com.example.websocket.service.GlobalWebSocketService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global WebSocket 제어를 위한 UseCase
 *
 * 앱 전체의 WebSocket 연결을 제어하는 기능을 제공한다.
 * 주로 앱 생명주기나 사용자 액션에 따른 WebSocket 상태 제어에 사용된다.
 */
@Singleton
class ControlGlobalWebSocketUseCase @Inject constructor(
    private val globalWebSocketService: GlobalWebSocketService
) {

    /**
     * WebSocket 서비스 시작
     */
    fun startService() {
        globalWebSocketService.startService()
    }

    /**
     * WebSocket 서비스 중지
     */
    fun stopService() {
        globalWebSocketService.stopService()
    }

    /**
     * WebSocket 서버 URL 설정 및 연결 준비
     */
    fun configure(serverUrl: String) {
        globalWebSocketService.configure(serverUrl)
    }

    /**
     * 강제 재연결 실행
     *
     * 네트워크 문제나 인증 오류 등으로 인한 연결 문제 해결 시 사용
     */
    fun forceReconnect() {
        globalWebSocketService.forceReconnect()
    }

    /**
     * 강제 연결 해제
     *
     * 사용자가 명시적으로 연결을 끊거나 앱 종료 시 사용
     */
    fun forceDisconnect() {
        globalWebSocketService.forceDisconnect()
    }

    /**
     * 재연결 상태 초기화
     *
     * 재연결 시도 횟수를 리셋하고 새로운 연결 시도를 위한 상태로 초기화
     */
    fun resetReconnectionState() {
        globalWebSocketService.resetReconnectionState()
    }

    /**
     * 앱이 포그라운드로 진입했을 때 호출
     *
     * 앱이 활성화되면 WebSocket 연결을 복구하거나 유지한다.
     */
    fun onAppForegrounded() {
        globalWebSocketService.onAppForegrounded()
    }

    /**
     * 앱이 백그라운드로 진입했을 때 호출
     *
     * 앱이 비활성화되면 WebSocket 연결을 유지하거나 정리한다.
     * (현재는 연결을 유지하도록 구현됨)
     */
    fun onAppBackgrounded() {
        globalWebSocketService.onAppBackgrounded()
    }
}