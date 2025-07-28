package com.example.data_core.di

import android.content.Context
import com.example.data_core.database.AppDatabase
import com.example.data_core.util.DebugChatLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 애플리케이션 데이터베이스를 위한 Hilt DI 모듈
 * AppDatabase와 관련 DAO들의 의존성 주입을 설정
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * AppDatabase 싱글톤 인스턴스 제공
     * @param context Application Context
     * @return AppDatabase 인스턴스
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * DebugChatLogger 제공
     * @param database AppDatabase 인스턴스
     * @return DebugChatLogger
     */
    @Provides
    @Singleton
    fun provideDebugChatLogger(database: AppDatabase): DebugChatLogger {
        return DebugChatLogger(database.messagesDao())
    }
}