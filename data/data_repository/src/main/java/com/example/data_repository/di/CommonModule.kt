package com.example.data_repository.di

import androidx.room.RoomDatabase
import com.example.data_datasource.database.AppDatabase
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.RoomSyncCursorStore
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncCursorStore
import com.example.orchestrator.DefaultSyncManager
import com.example.orchestrator.MessageSyncPortFactory
import com.example.orchestrator.TaskSyncPortFactory
import com.example.orchestrator.SyncManagerFactory
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
        messageDao: com.example.data_model.local.MessageDao,
        messageMapper: com.example.mapper.message.MessageMapper,
        outboxDao: OutboxDao
    ): MessageSyncPortFactory {
        return MessageSyncPortFactory(
            messageRemoteDataSource,
            messageDao,
            messageMapper,
            outboxDao,
        )
    }

    /**
     * TaskSyncPortFactory 제공
     */
    @Provides
    @Singleton
    fun provideTaskSyncPortFactory(
        taskRemoteDataSource: com.example.data_datasource.remote.TaskRemoteDataSource,
        taskDao: com.example.data_model.local.TaskDao,
        taskMapper: com.example.mapper.task.TaskMapper,
        outboxDao: OutboxDao
    ): TaskSyncPortFactory {
        return TaskSyncPortFactory(
            taskRemoteDataSource,
            taskDao,
            taskMapper,
            outboxDao,
        )
    }

    /**
     * SyncCoordinator 제공 (빈 포트)
     * 런타임 동기화는 SyncManagerFactory에서 스트림별 포트를 구성해 실행합니다.
     */
    @Provides
    @Singleton
    fun provideSyncCoordinator(
        cursorStore: SyncCursorStore
    ): SyncCoordinator {
        // 현재 MessageSyncPort는 런타임에 SyncManagerFactory에서 동적으로 생성합니다
        return DefaultSyncManager(
            ports = emptyList(),
            cursorStore = cursorStore,
            pageSize = 50 // 한 번에 동기화할 메시지 수
        )
    }

    /**
     * SyncManagerFactory 제공: 채널 컨텍스트별로 Message/Task 포트를 조립해 Coordinator 생성
     */
    @Provides
    @Singleton
    fun provideSyncManagerFactory(
        cursorStore: SyncCursorStore,
        messageSyncPortFactory: MessageSyncPortFactory,
        taskSyncPortFactory: TaskSyncPortFactory
    ): SyncManagerFactory {
        return SyncManagerFactory(cursorStore, messageSyncPortFactory, taskSyncPortFactory)
    }
}
