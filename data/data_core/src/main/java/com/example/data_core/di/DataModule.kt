package com.example.data_core.di

import com.example.data_core.util.CurrentUserProvider
import com.example.data_core.util.CurrentUserProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    /**
     * CurrentUserProvider 인터페이스 바인딩
     *
     * @param impl CurrentUserProviderImpl 구현체
     * @return CurrentUserProvider 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(
        impl: CurrentUserProviderImpl
    ): CurrentUserProvider
    
    // 기타 바인딩...

    // FirebaseStorage는 FirebaseModule에서 제공되므로 여기서는 제거함
}