package com.example.core_common.di

import com.example.core_common.dispatcher.DispatcherProvider
import com.example.core_common.dispatcher.DispatcherProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 코루틴 디스패처 관련 의존성을 Hilt에 바인딩하는 모듈입니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DispatchersModule {

    /**
     * DispatcherProvider 인터페이스 요청 시
     * DispatcherProviderImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DispatcherProviderImpl): DispatcherProvider
} 