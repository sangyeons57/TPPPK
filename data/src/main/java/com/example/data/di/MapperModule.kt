package com.example.data.di

import com.example.data.model.mapper.UserMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 매퍼 클래스를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    /**
     * UserMapper를 제공합니다.
     *
     * @return UserMapper 인스턴스
     */
    @Provides
    @Singleton
    fun provideUserMapper(): UserMapper {
        return UserMapper()
    }
} 