package com.example.data.di

import com.example.data.model.mapper.CategoryMapper
import com.example.data.model.mapper.ChannelMapper
import com.example.data.model.mapper.ChatMessageMapper
import com.example.data.model.mapper.MediaImageMapper
import com.example.data.model.mapper.UserMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.core_common.util.DateTimeUtil

/**
 * 매퍼 클래스를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    /**
     * UserMapper를 제공합니다.
     *
     * @return UserMapper 인스턴스
     */
    @Provides
    @Singleton
    fun provideUserMapper(dateTimeUtil: DateTimeUtil): UserMapper {
        return UserMapper(dateTimeUtil)
    }

    @Provides
    @Singleton
    fun provideChatMessageMapper(dateTimeUtil: DateTimeUtil): ChatMessageMapper {
        return ChatMessageMapper(dateTimeUtil)
    }

    @Provides
    @Singleton
    fun provideMediaImageMapper(dateTimeUtil: DateTimeUtil): MediaImageMapper {
        return MediaImageMapper(dateTimeUtil)
    }

    @Provides
    @Singleton
    fun provideChannelMapper(dateTimeUtil: DateTimeUtil): ChannelMapper {
        return ChannelMapper(dateTimeUtil)
    }

    @Provides
    @Singleton
    fun provideDateTimeUtil(): DateTimeUtil {
        return DateTimeUtil
    }
} 