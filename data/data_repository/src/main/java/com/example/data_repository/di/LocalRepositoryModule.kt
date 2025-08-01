package com.example.data_repository.di

import com.example.data_repository.local.LocalMessageRepositoryImpl
import com.example.data_repository.local.OutBoxRepositoryImpl
import com.example.data_repository.local.ScopeMetaDataRepositoryImpl
import com.example.domain_repository.local.LocalMessagePagingRepository
import com.example.domain_repository.local.LocalMessageRepository
import com.example.domain_repository.local.OutBoxRepository
import com.example.domain_repository.local.ScopeMetaDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocalMessageRepository(impl: LocalMessageRepositoryImpl): LocalMessageRepository

    @Binds
    @Singleton
    abstract fun bindLocalMessagePagingRepository(impl: LocalMessageRepositoryImpl): LocalMessagePagingRepository

    @Binds
    @Singleton
    abstract fun bindOutBoxRepository(impl: OutBoxRepositoryImpl): OutBoxRepository

    @Binds
    @Singleton
    abstract fun bindScopeMetaDataRepository(impl: ScopeMetaDataRepositoryImpl): ScopeMetaDataRepository

}