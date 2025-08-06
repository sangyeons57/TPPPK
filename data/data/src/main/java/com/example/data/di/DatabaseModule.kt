package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data_datasource.database.AppDatabase
import com.example.data_datasource.database.migration.MIGRATION_3_4
import com.example.data_model.local.MessageDao
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.SyncMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room Database와 DAO를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Room Database 제공
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "projecting_kotlin_database"
        )
            .addMigrations(MIGRATION_3_4) // channel_id 필드 추가 마이그레이션
            .fallbackToDestructiveMigration() // messageType + payload 전환을 위한 파괴적 마이그레이션 활성화
            .build()
    }

    /**
     * OutBoxDao 제공
     */
    @Provides
    @Singleton
    fun provideOutBoxDao(database: AppDatabase): OutboxDao {
        return database.outBoxDao()
    }

    /**
     * MessageDao 제공
     */
    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    /**
     * ScopeMetadataDao 제공
     */
    @Provides
    @Singleton
    fun provideScopeMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}