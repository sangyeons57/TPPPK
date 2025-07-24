package com.example.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.data.converter.InstantConverter
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.ChannelSyncDao
import com.example.data.model.local.ChatMessageEntity
import com.example.data.model.local.ChannelSyncEntity

/**
 * 채팅 로컬 저장을 위한 Room Database
 * updateAt 기반 증분 동기화와 효율적인 메시지 캐싱을 지원
 */
@Database(
    entities = [
        ChatMessageEntity::class,
        ChannelSyncEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(InstantConverter::class)
abstract class ChatDatabase : RoomDatabase() {

    /**
     * 채팅 메시지 DAO
     */
    abstract fun chatMessageDao(): ChatMessageDao

    /**
     * 채널 동기화 상태 DAO
     */
    abstract fun channelSyncDao(): ChannelSyncDao

    companion object {
        /**
         * 데이터베이스 이름
         */
        const val DATABASE_NAME = "chat_database"

        /**
         * 데이터베이스 싱글톤 인스턴스
         */
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        /**
         * 데이터베이스 인스턴스 생성/반환
         * @param context Application Context
         * @return ChatDatabase 인스턴스
         */
        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
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
         * @return 인메모리 ChatDatabase 인스턴스
         */
        fun getInMemoryDatabase(context: Context): ChatDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java
            )
                .addTypeConverter(InstantConverter())
                .allowMainThreadQueries() // 테스트에서만 허용
                .build()
        }
    }
}