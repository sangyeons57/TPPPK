package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.db.dao.*
import com.example.data.db.migration.AppDatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 모듈: Room 데이터베이스 및 DAO 인스턴스 제공
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Room 데이터베이스 인스턴스를 싱글톤으로 제공합니다.
     * 개발 환경에서는 스키마 변경 시 데이터베이스를 완전히 재생성하고,
     * 프로덕션 환경에서는 마이그레이션을 적용하여 데이터를 보존합니다.
     * 
     * @param appContext 애플리케이션 컨텍스트.
     * @return AppDatabase 인스턴스.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        // 프로덕션 환경에서는 마이그레이션 사용, 개발 환경에서는 destructive 방식 사용
        // BuildConfig.DEBUG를 사용할 경우 아래 조건문으로 변경 가능
        // if (BuildConfig.DEBUG) {
        //     fallbackToDestructiveMigration()
        // } else {
        //     addMigrations(*AppDatabaseMigrations.ALL_MIGRATIONS)
        // }
        
        // 현재는 개발 단계이므로 마이그레이션과 fallback 모두 적용
        // fallbackToDestructiveMigration()의 deprecated된 버전 대신 명시적으로 dropAllTables = true 설정
        .fallbackToDestructiveMigration(dropAllTables = true) // 마이그레이션 실패 시 모든 테이블 삭제 후 재생성
        .addMigrations(*AppDatabaseMigrations.ALL_MIGRATIONS) // 마이그레이션 적용
        .build()
    }

    /**
     * ScheduleDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return ScheduleDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideScheduleDao(appDatabase: AppDatabase): ScheduleDao {
        return appDatabase.scheduleDao()
    }

    /**
     * UserDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return UserDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    /**
     * ProjectDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return ProjectDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideProjectDao(appDatabase: AppDatabase): ProjectDao {
        return appDatabase.projectDao()
    }

    /**
     * ChatDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return ChatDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatDao {
        return appDatabase.chatDao()
    }
    
    /**
     * FriendDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return FriendDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideFriendDao(appDatabase: AppDatabase): FriendDao {
        return appDatabase.friendDao()
    }
    
    /**
     * DmDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return DmDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideDmDao(appDatabase: AppDatabase): DmDao {
        return appDatabase.dmDao()
    }
    
    /**
     * RoleDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return RoleDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideRoleDao(appDatabase: AppDatabase): RoleDao {
        return appDatabase.roleDao()
    }
    
    /**
     * ProjectMemberDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return ProjectMemberDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideProjectMemberDao(appDatabase: AppDatabase): ProjectMemberDao {
        return appDatabase.projectMemberDao()
    }
    
    /**
     * ProjectStructureDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return ProjectStructureDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideProjectStructureDao(appDatabase: AppDatabase): ProjectStructureDao {
        return appDatabase.projectStructureDao()
    }
    
    /**
     * InviteDao 인스턴스를 싱글톤으로 제공합니다.
     * @param appDatabase AppDatabase 인스턴스.
     * @return InviteDao 인스턴스.
     */
    @Provides
    @Singleton
    fun provideInviteDao(appDatabase: AppDatabase): InviteDao {
        return appDatabase.inviteDao()
    }
} 