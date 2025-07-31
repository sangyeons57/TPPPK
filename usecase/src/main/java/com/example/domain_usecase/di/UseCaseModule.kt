package com.example.domain_usecase.di

import com.example.domain_usecase.sync.SyncManager
import com.example.domain_usecase.sync.SyncManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * UseCase 계층의 의존성 주입을 위한 Hilt 모듈
 *
 * UseCase 계층의 비즈니스 로직 컴포넌트들을 바인딩합니다.
 * Repository를 주입받아 복합적인 비즈니스 로직을 처리하는 Manager들이 포함됩니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    /**
     * SyncManager 인터페이스 바인딩
     *
     * OutBoxRepository와 ScopeMetaDataRepository를 조합하여
     * 동기화 관련 복합 비즈니스 로직을 처리합니다.
     *
     * @param impl SyncManagerImpl 구현체
     * @return SyncManager 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindSyncManager(
        impl: SyncManagerImpl
    ): SyncManager
}