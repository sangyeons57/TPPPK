package com.example.mapper.di

import com.example.mapper.CategoryEntityMapper
import com.example.mapper.CategoryEntityMapperImpl
import com.example.mapper.DMChannelEntityMapper
import com.example.mapper.DMChannelEntityMapperImpl
import com.example.mapper.DMWrapperEntityMapper
import com.example.mapper.DMWrapperEntityMapperImpl
import com.example.mapper.FriendEntityMapper
import com.example.mapper.FriendEntityMapperImpl
import com.example.mapper.MemberEntityMapper
import com.example.mapper.MemberEntityMapperImpl
import com.example.mapper.MessageAttachmentEntityMapper
import com.example.mapper.MessageAttachmentEntityMapperImpl
import com.example.mapper.MessageEntityMapper
import com.example.mapper.MessageEntityMapperImpl
import com.example.mapper.PermissionEntityMapper
import com.example.mapper.PermissionEntityMapperImpl
import com.example.mapper.ProjectChannelEntityMapper
import com.example.mapper.ProjectChannelEntityMapperImpl
import com.example.mapper.ProjectInvitationEntityMapper
import com.example.mapper.ProjectInvitationEntityMapperImpl
import com.example.mapper.ProjectEntityMapper
import com.example.mapper.ProjectEntityMapperImpl
import com.example.mapper.RoleEntityMapper
import com.example.mapper.RoleEntityMapperImpl
import com.example.mapper.ProjectsWrapperEntityMapper
import com.example.mapper.ProjectsWrapperEntityMapperImpl
import com.example.mapper.ReactionEntityMapper
import com.example.mapper.ReactionEntityMapperImpl
import com.example.mapper.ScheduleEntityMapper
import com.example.mapper.ScheduleEntityMapperImpl
import com.example.mapper.TaskEntityMapper
import com.example.mapper.TaskEntityMapperImpl
import com.example.mapper.UserEntityMapper
import com.example.mapper.UserEntityMapperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EntityMapperModule {

    @Binds
    abstract fun bindCategoryEntityMapper(mapper: CategoryEntityMapperImpl): CategoryEntityMapper

    @Binds
    abstract fun bindFriendEntityMapper(mapper: FriendEntityMapperImpl): FriendEntityMapper

    @Binds
    abstract fun bindDMChannelEntityMapper(mapper: DMChannelEntityMapperImpl): DMChannelEntityMapper

    @Binds
    abstract fun bindDMWrapperEntityMapper(mapper: DMWrapperEntityMapperImpl): DMWrapperEntityMapper

    @Binds
    abstract fun bindMemberEntityMapper(mapper: MemberEntityMapperImpl): MemberEntityMapper

    @Binds
    abstract fun bindMessageAttachmentEntityMapper(mapper: MessageAttachmentEntityMapperImpl): MessageAttachmentEntityMapper

    @Binds
    abstract fun bindMessageEntityMapper(mapper: MessageEntityMapperImpl): MessageEntityMapper

    @Binds
    abstract fun bindPermissionEntityMapper(mapper: PermissionEntityMapperImpl): PermissionEntityMapper

    @Binds
    abstract fun bindProjectChannelEntityMapper(mapper: ProjectChannelEntityMapperImpl): ProjectChannelEntityMapper

    @Binds
    abstract fun bindProjectInvitationEntityMapper(mapper: ProjectInvitationEntityMapperImpl): ProjectInvitationEntityMapper

    @Binds
    abstract fun bindProjectEntityMapper(mapper: ProjectEntityMapperImpl): ProjectEntityMapper

    @Binds
    abstract fun bindRoleEntityMapper(mapper: RoleEntityMapperImpl): RoleEntityMapper

    @Binds
    abstract fun bindProjectsWrapperEntityMapper(mapper: ProjectsWrapperEntityMapperImpl): ProjectsWrapperEntityMapper

    @Binds
    abstract fun bindReactionEntityMapper(mapper: ReactionEntityMapperImpl): ReactionEntityMapper

    @Binds
    abstract fun bindScheduleEntityMapper(mapper: ScheduleEntityMapperImpl): ScheduleEntityMapper

    @Binds
    abstract fun bindTaskEntityMapper(mapper: TaskEntityMapperImpl): TaskEntityMapper

    @Binds
    abstract fun bindUserEntityMapper(mapper: UserEntityMapperImpl): UserEntityMapper

}
