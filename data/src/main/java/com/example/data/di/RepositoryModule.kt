package com.example.data.di

import com.example.data.repository.ChatCacheRepositoryImpl
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.repository.ChatCacheRepository
import com.example.domain.repository.DefaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindDefaultRepository(impl: DefaultRepositoryImpl): DefaultRepository

    @Binds
    abstract fun bindChatCacheRepository(impl: ChatCacheRepositoryImpl): ChatCacheRepository
}