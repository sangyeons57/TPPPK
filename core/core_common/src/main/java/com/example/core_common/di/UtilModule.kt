package com.example.core_common.di

import com.example.core_common.util.AuthUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideAuthUtil(): AuthUtil {
        return AuthUtil
    }
}
