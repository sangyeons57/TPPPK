package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data_datasource.database.AppDatabase
import com.example.data_datasource.database.migration.DatabaseMigrations
import com.example.data_model.local.MessageDao
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.SyncMetadataDao
import com.example.data_model.local.TaskDao
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
        // 마이그레이션 체인 검증 (개발 모드에서만)
        // BuildConfig 대신 시스템 속성 사용
        val isDebug = System.getProperty("debug.mode", "false").toBoolean() ||
                context.applicationInfo?.let { (it.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 } ?: false

        if (isDebug) {
            DatabaseMigrations.validateMigrationChain()?.let { error ->
                throw IllegalStateException("마이그레이션 검증 실패: $error")
            }
        }
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "projecting_kotlin_database"
        )
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS) // 자동화된 마이그레이션 관리
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
     * SyncMetadataDao 제공
     */
    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    /**
     * TaskDao 제공
     */
    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }
}
