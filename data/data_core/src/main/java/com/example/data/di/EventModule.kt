package com.example.data.di

import com.example.domain.event.DomainEventPublisher
import com.example.domain.event.EventDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the in-memory [DomainEventPublisher] implementation used across the application.
 * Currently binds the singleton [EventDispatcher] object. Swap this provider when migrating
 * to an external message bus.
 */
@Module
@InstallIn(SingletonComponent::class)
object EventModule {

    @Provides
    @Singleton
    fun provideDomainEventPublisher(): DomainEventPublisher = EventDispatcher
}
