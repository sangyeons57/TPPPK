package com.example.core_fcm.di

import android.content.Context
import com.example.core_fcm.FCMInitializer
import com.example.core_fcm.utils.FCMTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FCM 관련 의존성을 제공하는 Dagger-Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object FCMModule {
    
    /**
     * FCM 초기화 클래스 제공
     */
    @Provides
    @Singleton
    fun provideFCMInitializer(
        @ApplicationContext context: Context
    ): FCMInitializer {
        return FCMInitializer(context)
    }
    
    /**
     * FCM 토큰 관리자 제공
     */
    @Provides
    @Singleton
    fun provideFCMTokenManager(
        @ApplicationContext context: Context
    ): FCMTokenManager {
        return FCMTokenManager(context)
    }
} 