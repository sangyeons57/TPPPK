package com.example.data.di

import com.example.data.util.CurrentUserProvider
import com.example.data.util.CurrentUserProviderImpl
import com.example.data.util.NetworkConnectivityMonitorImpl
import com.example.domain.util.NetworkConnectivityMonitor
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
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
    @Binds
    abstract fun bindNetworkConnectivityMonitor(
        impl: NetworkConnectivityMonitorImpl
    ): NetworkConnectivityMonitor
    
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

    // FirebaseStorage는 구체적인 인스턴스를 직접 생성하여 제공해야 하므로 @Provides 사용
    // abstract class 내의 @Provides 메소드는 companion object 안에 위치해야 함
    companion object {
        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage {
            return FirebaseStorage.getInstance()
        }
    }
}