package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data_datasource.dao.OutBoxDao
import com.example.data_datasource.dao.ScopeMetadataDao
import com.example.data_datasource.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room Database와 DAO를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Room Database 제공
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "projecting_kotlin_database"
        ).build()
    }

    /**
     * OutBoxDao 제공
     */
    @Provides
    @Singleton
    fun provideOutBoxDao(database: AppDatabase): OutBoxDao {
        return database.outBoxDao()
    }

    /**
     * ScopeMetadataDao 제공
     */
    @Provides
    @Singleton
    fun provideScopeMetadataDao(database: AppDatabase): ScopeMetadataDao {
        return database.scopeMetadataDao()
    }
}