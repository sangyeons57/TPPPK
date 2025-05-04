package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.db.dao.*
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
        DmConversationEntity::class,
        RoleEntity::class,
        RolePermissionEntity::class,
        ProjectMemberEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        InviteEntity::class
    ],
    version = 7, // 버전 6에서 7로 업데이트 (InviteEntity에 inviterName, projectName 필드 추가)
    exportSchema = true // 스키마 내보내기 활성화 (마이그레이션 테스트 및 관리를 위해 필요)
)
@TypeConverters(AppTypeConverters::class)
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
     * DM 대화 테이블에 접근하기 위한 DAO를 제공합니다.
     */
    abstract fun dmDao(): DmDao
    
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

    companion object {
        const val DATABASE_NAME = "app_database" // 데이터베이스 파일 이름
    }
} 