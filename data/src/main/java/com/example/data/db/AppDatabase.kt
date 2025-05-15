package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
// import com.example.data.db.converter.ListConverter // 불필요한 임포트 제거
import com.example.data.db.dao.*
import com.example.data.db.migration.ChannelMigration
import com.example.data.model.local.*
import com.example.data.model.local.chat.ChatMessageEntity

/**
 * 앱의 Room 데이터베이스 클래스입니다.
 * 데이터베이스 버전 관리, 엔티티 및 DAO 정의, TypeConverter 등록 등을 담당합니다.
 */
@Database(
    entities = [
        ScheduleEntity::class,
        UserEntity::class,
        ProjectEntity::class,
        ChatEntity::class,
        ChatMessageEntity::class,
        FriendEntity::class,
        FriendRequestEntity::class,
        RoleEntity::class,
        RolePermissionEntity::class,
        ProjectMemberEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        InviteEntity::class
    ],
    version = 8, // 버전 7에서 8로 업데이트 (ChannelEntity 구조 변경)
    exportSchema = true // 스키마 내보내기 활성화 (마이그레이션 테스트 및 관리를 위해 필요)
)
@TypeConverters(AppTypeConverters::class /*, ListConverter::class*/)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Schedule 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun scheduleDao(): ScheduleDao

    /**
     * User 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun userDao(): UserDao

    /**
     * Project 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun projectDao(): ProjectDao

    /**
     * Chat 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun chatDao(): ChatDao
    
    /**
     * Friend 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun friendDao(): FriendDao
    
    /**
     * Role 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun roleDao(): RoleDao
    
    /**
     * ProjectMember 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun projectMemberDao(): ProjectMemberDao
    
    /**
     * ProjectStructure 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun projectStructureDao(): ProjectStructureDao
    
    /**
     * Invite 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun inviteDao(): InviteDao

    /**
     * Channel 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun channelDao(): ChannelDao

    companion object {
        const val DATABASE_NAME = "app_database" // 데이터베이스 파일 이름
        
        /**
         * 데이터베이스 마이그레이션 스크립트 목록
         */
        val MIGRATIONS = arrayOf(
            // 버전 7에서 8로 마이그레이션 (Channel 구조 변경)
            ChannelMigration.MIGRATION_2_3
        )
    }

    // 데이터베이스 빌더에 fallbackToDestructiveMigration() 추가
    // [Improvement, 스키마 변경 시 데이터 손실 없이 마이그레이션을 처리해야 함, 실제 앱에서는 데이터 마이그레이션 로직 구현 필요]
    // TODO: Implement proper database migration instead of destructive migration
} 