package com.example.data_datasource.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_model.local.MessageDao
import com.example.data_model.local.MessageEntity
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.OutboxRecordEntity
import com.example.data_model.local.SyncMetadataDao
import com.example.data_model.local.SyncMetadataEntity

/**
 * Room Database 설정
 */
@Database(
    entities = [
        MessageEntity::class,
        OutboxRecordEntity::class,
        SyncMetadataEntity::class
    ],
    version = 8, // v8: messages(channelId, createdAt) 복합 인덱스 추가
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun outBoxDao(): OutboxDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}