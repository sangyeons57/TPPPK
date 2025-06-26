package com.example.data.di

import com.example.data.repository.base.FunctionsRepositoryImpl
import com.example.domain.repository.FunctionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 관련 의존성을 Hilt에 바인딩하는 모듈입니다.
 * 팩토리 패턴이 필요하지 않은 단순한 Repository 구현체들을 바인딩합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * FunctionsRepository 인터페이스 요청 시
     * FunctionsRepositoryImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindFunctionsRepository(
        functionsRepositoryImpl: FunctionsRepositoryImpl
    ): FunctionsRepository
}