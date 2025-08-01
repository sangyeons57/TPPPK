package com.example.websocket

import com.example.websocket.core.WebSocketManager
import com.example.websocket.core.WebSocketManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WebSocket 모듈의 의존성 주입 설정
 *
 * 새로 정리된 패키지 구조에 맞춰 업데이트된 DI 모듈
 * - core: 핵심 인프라 (WebSocketManager, WebSocketManagerImpl)
 * - service: 서비스 계층 (GlobalWebSocketService, WebSocketMessageService)
 * - event: 이벤트 처리 (WebSocketDomainEvent, WebSocketEventFlow, WebSocketDomainMapper)
 * - usecase: 유스케이스 (WebSocketUseCaseProvider, GlobalWebSocketUseCaseProvider)
 * - constant: 상수들 (WebSocketEventTypes, OperationStatus)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WebSocketModule {

    @Binds
    @Singleton
    abstract fun bindWebSocketManager(
        webSocketManagerImpl: WebSocketManagerImpl
    ): WebSocketManager
}