package com.example.data_core.di

import com.example.data_core.repository.factory.AuthRepositoryFactoryImpl
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 인스턴스를 제공하는 Hilt 모듈
 * 특정 서비스들이 Repository를 직접 주입받을 수 있도록 합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * GlobalWebSocketService를 위한 AuthRepository 제공
     * Factory 패턴을 통해 기본 컨텍스트로 생성합니다.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        authRepositoryFactory: AuthRepositoryFactoryImpl
    ): AuthRepository {
        // 기본 컨텍스트로 AuthRepository 생성
        val defaultContext = AuthRepositoryFactoryContext()
        return authRepositoryFactory.create(defaultContext)
    }
}
