package com.example.data.di

import com.example.data.datasource.local.media.LocalMediaDataSource
import com.example.data.datasource.local.media.LocalMediaDataSourceImpl
import com.example.data.repository.MediaRepositoryImpl
import com.example.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 미디어 관련 의존성을 제공하는 Hilt 모듈입니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    /**
     * LocalMediaDataSource의 구현체를 바인딩합니다.
     */
    @Binds
    @Singleton
    abstract fun bindLocalMediaDataSource(impl: LocalMediaDataSourceImpl): LocalMediaDataSource

    /**
     * MediaRepository의 구현체를 바인딩합니다.
     */
    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
} 