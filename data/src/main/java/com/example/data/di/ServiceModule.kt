package com.example.data.di

import com.example.data.service.CacheService
import com.example.data.service.CacheServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 서비스 관련 의존성 주입을 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    /**
     * CacheService 구현체를 바인딩합니다.
     *
     * @param cacheServiceImpl CacheService 구현체
     * @return CacheService 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindCacheService(cacheServiceImpl: CacheServiceImpl): CacheService
} 