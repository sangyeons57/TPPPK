package com.example.data_repository.di

import androidx.room.RoomDatabase
import com.example.data_datasource.database.AppDatabase
import com.example.data_model.local.RoomSyncCursorStore
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncCursorStore
import com.example.orchestrator.DefaultSyncManager
import com.example.orchestrator.MessageSyncPortFactory
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
     * SyncCursorStore 제공
     * Room DB 기반 커서 저장소
     */
    @Provides
    @Singleton
    fun provideSyncCursorStore(database: AppDatabase): SyncCursorStore {
        return RoomSyncCursorStore(database.syncMetadataDao())
    }

    /**
     * MessageSyncPortFactory 제공
     * 채널별 MessageSyncPort 생성 팩토리
     */
    @Provides
    @Singleton
    fun provideMessageSyncPortFactory(
        messageRemoteDataSource: com.example.data_datasource.remote.MessageRemoteDataSource,
        messageRepository: com.example.domain_repository.base.MessageRepository
    ): MessageSyncPortFactory {
        return MessageSyncPortFactory(messageRemoteDataSource, messageRepository)
    }

    /**
     * SyncCoordinator 제공
     * DefaultSyncManager 구현체로 실제 증분 동기화 기능 제공
     */
    @Provides
    @Singleton
    fun provideSyncCoordinator(
        cursorStore: SyncCursorStore
    ): SyncCoordinator {
        // 현재 MessageSyncPort는 런타임에 동적으로 생성되므로 빈 리스트로 초기화
        // 실제 동기화는 SyncUseCase에서 채널별로 처리됨
        return DefaultSyncManager(
            ports = emptyList(),
            cursorStore = cursorStore,
            pageSize = 50 // 한 번에 동기화할 메시지 수
        )
    }
} 