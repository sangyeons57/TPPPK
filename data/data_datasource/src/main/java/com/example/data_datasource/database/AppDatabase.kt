package com.example.data_datasource.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data_datasource.dao.OutBoxDao
import com.example.data_datasource.dao.ScopeMetadataDao
import com.example.data_model.local.OutBoxEntity
import com.example.data_model.local.ScopeMetadataEntity

/**
 * Room Database 설정
 */
@Database(
    entities = [
        OutBoxEntity::class,
        ScopeMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun outBoxDao(): OutBoxDao
    abstract fun scopeMetadataDao(): ScopeMetadataDao
}