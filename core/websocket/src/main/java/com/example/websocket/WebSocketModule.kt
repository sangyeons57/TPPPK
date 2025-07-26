package com.example.websocket

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebSocketModule {

    @Binds
    @Singleton
    abstract fun bindWebSocketManager(
        webSocketManagerImpl: WebSocketManagerImpl
    ): WebSocketManager
}