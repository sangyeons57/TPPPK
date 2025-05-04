package com.example.data.di

import com.example.data.util.NetworkConnectivityMonitorImpl
import com.example.domain.util.NetworkConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 데이터 레이어의 기본 의존성 주입을 위한 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * NetworkConnectivityMonitor 인터페이스 바인딩
     *
     * @param impl NetworkConnectivityMonitorImpl 구현체
     * @return NetworkConnectivityMonitor 인터페이스
     */
    @Binds
    abstract fun bindNetworkConnectivityMonitor(
        impl: NetworkConnectivityMonitorImpl
    ): NetworkConnectivityMonitor
    
    // 기타 바인딩...
} 