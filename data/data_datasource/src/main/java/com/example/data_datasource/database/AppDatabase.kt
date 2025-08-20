package com.example.data_datasource.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_model.local.MessageDao
import com.example.data_model.local.MessageEntity
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.OutboxRecordEntity
import com.example.data_model.local.SyncMetadataDao
import com.example.data_model.local.SyncMetadataEntity
import com.example.data_model.local.TaskDao
import com.example.data_model.local.TaskEntity

/**
 * Room Database 설정
 */
@Database(
    entities = [
        MessageEntity::class,
        OutboxRecordEntity::class,
        SyncMetadataEntity::class,
        TaskEntity::class,
    ],
    version = 9, // v9: tasks 테이블 추가
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun outBoxDao(): OutboxDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun taskDao(): TaskDao
}
