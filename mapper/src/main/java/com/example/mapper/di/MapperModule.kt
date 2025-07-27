package com.example.mapper.di

import com.example.mapper.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    @Provides
    @Singleton
    fun provideCategoryMapper(): CategoryMapper = CategoryMapperImpl()

    @Provides
    @Singleton
    fun provideDMChannelMapper(): DMChannelMapper = DMChannelMapperImpl()

    @Provides
    @Singleton
    fun provideDMWrapperMapper(): DMWrapperMapper = DMWrapperMapperImpl()

    @Provides
    @Singleton
    fun provideFriendMapper(): FriendMapper = FriendMapperImpl()

    @Provides
    @Singleton
    fun provideMemberMapper(): MemberMapper = MemberMapperImpl()

    @Provides
    @Singleton
    fun provideMessageAttachmentMapper(): MessageAttachmentMapper = MessageAttachmentMapperImpl()

    @Provides
    @Singleton
    fun provideMessageMapper(): MessageMapper = MessageMapperImpl()

    @Provides
    @Singleton
    fun providePermissionMapper(): PermissionMapper = PermissionMapperImpl()

    @Provides
    @Singleton
    fun provideProjectChannelMapper(): ProjectChannelMapper = ProjectChannelMapperImpl()

    @Provides
    @Singleton
    fun provideProjectInvitationMapper(): ProjectInvitationMapper = ProjectInvitationMapperImpl()

    @Provides
    @Singleton
    fun provideProjectMapper(): ProjectMapper = ProjectMapperImpl()

    @Provides
    @Singleton
    fun provideProjectsWrapperMapper(): ProjectsWrapperMapper = ProjectsWrapperMapperImpl()

    @Provides
    @Singleton
    fun provideReactionMapper(): ReactionMapper = ReactionMapperImpl()

    @Provides
    @Singleton
    fun provideRoleMapper(): RoleMapper = RoleMapperImpl()

    @Provides
    @Singleton
    fun provideScheduleMapper(): ScheduleMapper = ScheduleMapperImpl()

    @Provides
    @Singleton
    fun provideTaskMapper(): TaskMapper = TaskMapperImpl()

    @Provides
    @Singleton
    fun provideUserMapper(): UserMapper = UserMapperImpl()
}
