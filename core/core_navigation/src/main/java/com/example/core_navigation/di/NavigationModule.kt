package com.example.core_navigation.di

import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.NavigationManagerImpl
import com.example.core_navigation.core.NavigationResultManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Navigation module providing dependencies for the refactored navigation system.
 * 
 * This module provides:
 * - NavigationResultManager for type-safe result handling
 * - NavigationManager as the primary navigation implementation
 * - AppNavigator interface binding
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    
    /**
     * Binds NavigationManager to AppNavigator interface
     */
    @Binds
    @Singleton
    abstract fun bindAppNavigator(
        manager: NavigationManagerImpl
    ): NavigationManger
    
    companion object {
        
        /**
         * Provides NavigationResultManager singleton.
         * This is provided as a separate singleton to allow direct injection
         * in ViewModels and other components that need result handling.
         */
        @Provides
        @Singleton
        fun provideNavigationResultManager(): NavigationResultManager {
            return NavigationResultManager()
        }
    }
} 