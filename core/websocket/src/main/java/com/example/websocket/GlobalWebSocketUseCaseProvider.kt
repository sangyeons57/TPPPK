package com.example.websocket

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 글로벌 WebSocket 관련 UseCase들을 제공하는 Provider
 *
 * 앱 전체의 WebSocket 연결 상태 모니터링 및 제어 기능을 담당합니다.
 */
@Singleton
class GlobalWebSocketUseCaseProvider @Inject constructor(
    private val getGlobalWebSocketStatusUseCase: GetGlobalWebSocketStatusUseCase,
    private val controlGlobalWebSocketUseCase: ControlGlobalWebSocketUseCase
) {

    /**
     * 글로벌 WebSocket 관련 UseCase들을 생성합니다.
     *
     * @return 글로벌 WebSocket UseCase 그룹
     */
    fun create(): GlobalWebSocketUseCases {
        return GlobalWebSocketUseCases(
            getGlobalWebSocketStatusUseCase = getGlobalWebSocketStatusUseCase,
            controlGlobalWebSocketUseCase = controlGlobalWebSocketUseCase
        )
    }
}

/**
 * 글로벌 WebSocket UseCase 그룹
 */
data class GlobalWebSocketUseCases(
    val getGlobalWebSocketStatusUseCase: GetGlobalWebSocketStatusUseCase,
    val controlGlobalWebSocketUseCase: ControlGlobalWebSocketUseCase
)