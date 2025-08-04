package com.example.data_repository.di

import androidx.room.RoomDatabase
import com.example.data_datasource.database.AppDatabase
import com.example.domain.model.sync.SyncCoordinator
import com.example.orchestrator.NoOpSyncCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * 공통 의존성들을 제공하는 Hilt 모듈
 * MessageRepositoryImpl에서 필요한 RoomDatabase와 Json 의존성을 제공합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    /**
     * RoomDatabase 제공
     * AppDatabase를 RoomDatabase 타입으로 바인딩
     */
    @Provides
    @Singleton
    fun provideRoomDatabase(database: AppDatabase): RoomDatabase {
        return database
    }

    /**
     * kotlinx.serialization.json.Json 제공
     * 기본 설정으로 Json 인스턴스를 생성
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    /**
     * SyncCoordinator 제공
     * NoOp 구현체를 제공하여 복잡한 동기화 로직 없이도 DI 오류를 해결
     */
    @Provides
    @Singleton
    fun provideSyncCoordinator(): SyncCoordinator {
        return NoOpSyncCoordinator()
    }
} 