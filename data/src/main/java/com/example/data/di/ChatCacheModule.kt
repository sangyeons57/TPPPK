package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data.cache.ChatCacheManager
import com.example.data.cache.ChatCacheManagerImpl
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.ChannelSyncDao
import com.example.data.database.ChatDatabase
import com.example.data.datasource.local.LocalChatDataSource
import com.example.data.datasource.local.LocalChatDataSourceImpl
import com.example.data.utils.DebugChatLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 채팅 로컬 캐시 시스템을 위한 Hilt DI 모듈
 * Room Database, DataSource, CacheManager의 의존성 주입을 설정
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatCacheModule {

    companion object {
        /**
         * ChatDatabase 싱글톤 인스턴스 제공
         * @param context Application Context
         * @return ChatDatabase 인스턴스
         */
        @Provides
        @Singleton
        fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                ChatDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true) // 개발 단계에서만 사용
                .build()
        }

        /**
         * ChatMessageDao 제공
         * @param database ChatDatabase 인스턴스
         * @return ChatMessageDao
         */
        @Provides
        fun provideChatMessageDao(database: ChatDatabase): ChatMessageDao {
            return database.chatMessageDao()
        }

        /**
         * ChannelSyncDao 제공
         * @param database ChatDatabase 인스턴스
         * @return ChannelSyncDao
         */
        @Provides
        fun provideChannelSyncDao(database: ChatDatabase): ChannelSyncDao {
            return database.channelSyncDao()
        }

        @Provides
        @Singleton
        fun provideDebugChatLogger(chatMessageDao: ChatMessageDao): DebugChatLogger {
            return DebugChatLogger(chatMessageDao)
        }
    }

    /**
     * LocalChatDataSource 인터페이스를 구현체와 바인딩
     * @param impl LocalChatDataSourceImpl 구현체
     * @return LocalChatDataSource 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindLocalChatDataSource(
        impl: LocalChatDataSourceImpl
    ): LocalChatDataSource

    /**
     * ChatCacheManager 인터페이스를 구현체와 바인딩
     * @param impl ChatCacheManagerImpl 구현체
     * @return ChatCacheManager 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindChatCacheManager(
        impl: ChatCacheManagerImpl
    ): ChatCacheManager
}