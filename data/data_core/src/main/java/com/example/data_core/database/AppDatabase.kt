package com.example.data_core.database

// Domain Entities

// Generic Outbox Entity

// Domain DAOs

// Generic Outbox DAO
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data_core.dao.CategoriesDao
import com.example.data_core.dao.DmChannelsDao
import com.example.data_core.dao.DmWrapperDao
import com.example.data_core.dao.FriendsDao
import com.example.data_core.dao.MembersDao
import com.example.data_core.dao.MessageAttachmentsDao
import com.example.data_core.dao.MessagesDao
import com.example.data_core.dao.PermissionsDao
import com.example.data_core.dao.ProjectChannelsDao
import com.example.data_core.dao.ProjectInvitationsDao
import com.example.data_core.dao.ProjectsDao
import com.example.data_core.dao.ProjectsWrapperDao
import com.example.data_core.dao.ReactionsDao
import com.example.data_core.dao.RolesDao
import com.example.data_core.dao.SchedulesDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.dao.TasksDao
import com.example.data_core.dao.UsersDao
import com.example.data_core.util.InstantConverter
import com.example.data_model.local.CategoriesEntity
import com.example.data_model.local.DmChannelsEntity
import com.example.data_model.local.DmWrapperEntity
import com.example.data_model.local.FriendsEntity
import com.example.data_model.local.MembersEntity
import com.example.data_model.local.MessageAttachmentsEntity
import com.example.data_model.local.MessagesEntity
import com.example.data_model.local.OutboxEntity
import com.example.data_model.local.PermissionsEntity
import com.example.data_model.local.ProjectChannelsEntity
import com.example.data_model.local.ProjectInvitationsEntity
import com.example.data_model.local.ProjectsEntity
import com.example.data_model.local.ProjectsWrapperEntity
import com.example.data_model.local.ReactionsEntity
import com.example.data_model.local.RolesEntity
import com.example.data_model.local.SchedulesEntity
import com.example.data_model.local.SyncMetadataEntity
import com.example.data_model.local.TasksEntity
import com.example.data_model.local.UsersEntity

/**
 * 애플리케이션 메인 Room Database
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다:
 * 1. Domain Entity - 순수 비즈니스 데이터 (UI용 SSOT)
 * 2. Generic Outbox Entity - 로컬 변경사항 추적 (서버 동기화 대기열)
 * 3. SyncMetadata - 증분 동기화 메타데이터 (커서 및 타임스탬프)
 */
@Database(
    entities = [
        // === Domain Entities (17) ===
        UsersEntity::class,
        ProjectsEntity::class,
        ProjectChannelsEntity::class,
        MessagesEntity::class,
        MessageAttachmentsEntity::class,
        ReactionsEntity::class,
        DmChannelsEntity::class,
        DmWrapperEntity::class,
        FriendsEntity::class,
        SchedulesEntity::class,
        TasksEntity::class,
        CategoriesEntity::class,
        MembersEntity::class,
        RolesEntity::class,
        PermissionsEntity::class,
        ProjectInvitationsEntity::class,
        ProjectsWrapperEntity::class,

        // === Generic Outbox Entity (1) ===
        OutboxEntity::class,

        // === Sync Metadata Entity (1) ===
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(InstantConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract val ProjectsEntity: Any

    // === Domain DAOs (17) ===
    abstract fun usersDao(): UsersDao
    abstract fun projectsDao(): ProjectsDao
    abstract fun projectChannelsDao(): ProjectChannelsDao
    abstract fun messagesDao(): MessagesDao
    abstract fun messageAttachmentsDao(): MessageAttachmentsDao
    abstract fun reactionsDao(): ReactionsDao
    abstract fun dmChannelsDao(): DmChannelsDao
    abstract fun dmWrapperDao(): DmWrapperDao
    abstract fun friendsDao(): FriendsDao
    abstract fun schedulesDao(): SchedulesDao
    abstract fun tasksDao(): TasksDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun membersDao(): MembersDao
    abstract fun rolesDao(): RolesDao
    abstract fun permissionsDao(): PermissionsDao
    abstract fun projectInvitationsDao(): ProjectInvitationsDao
    abstract fun projectsWrapperDao(): ProjectsWrapperDao

    // === Generic Outbox DAO (1) ===
    abstract fun outboxDao(): OutboxDao

    // === Sync Metadata DAO (1) ===
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        /**
         * 데이터베이스 이름
         */
        const val DATABASE_NAME = "app_database"

        /**
         * 데이터베이스 싱글톤 인스턴스
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 데이터베이스 인스턴스 생성/반환
         * @param context Application Context
         * @return AppDatabase 인스턴스
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addTypeConverter(InstantConverter())
                    .fallbackToDestructiveMigration(dropAllTables = true) // 개발 단계에서만 사용
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 테스트용 인메모리 데이터베이스 생성
         * @param context Test Context
         * @return 인메모리 AppDatabase 인스턴스
         */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .addTypeConverter(InstantConverter())
                .allowMainThreadQueries() // 테스트에서만 허용
                .build()
        }
    }
}