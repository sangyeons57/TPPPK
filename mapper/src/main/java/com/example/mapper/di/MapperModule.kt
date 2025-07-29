package com.example.mapper.di

import com.example.mapper.CategoryMapper
import com.example.mapper.CategoryMapperImpl
import com.example.mapper.DMChannelMapper
import com.example.mapper.DMChannelMapperImpl
import com.example.mapper.DMWrapperMapper
import com.example.mapper.DMWrapperMapperImpl
import com.example.mapper.FriendMapper
import com.example.mapper.FriendMapperImpl
import com.example.mapper.MemberMapper
import com.example.mapper.MemberMapperImpl
import com.example.mapper.MessageAttachmentMapper
import com.example.mapper.MessageAttachmentMapperImpl
import com.example.mapper.MessageMapper
import com.example.mapper.MessageMapperImpl
import com.example.mapper.PermissionMapper
import com.example.mapper.PermissionMapperImpl
import com.example.mapper.ProjectChannelMapper
import com.example.mapper.ProjectChannelMapperImpl
import com.example.mapper.ProjectInvitationMapper
import com.example.mapper.ProjectInvitationMapperImpl
import com.example.mapper.ProjectMapper
import com.example.mapper.ProjectMapperImpl
import com.example.mapper.ProjectsWrapperMapper
import com.example.mapper.ProjectsWrapperMapperImpl
import com.example.mapper.ReactionMapper
import com.example.mapper.ReactionMapperImpl
import com.example.mapper.RoleMapper
import com.example.mapper.RoleMapperImpl
import com.example.mapper.ScheduleMapper
import com.example.mapper.ScheduleMapperImpl
import com.example.mapper.TaskMapper
import com.example.mapper.TaskMapperImpl
import com.example.mapper.UserMapper
import com.example.mapper.UserMapperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MapperModule {

    @Binds
    abstract fun bindCategoryMapper(mapper: CategoryMapperImpl): CategoryMapper

    @Binds
    abstract fun bindDMChannelMapper(mapper: DMChannelMapperImpl): DMChannelMapper

    @Binds
    abstract fun bindDMWrapperMapper(mapper: DMWrapperMapperImpl): DMWrapperMapper

    @Binds
    abstract fun bindFriendMapper(mapper: FriendMapperImpl): FriendMapper

    @Binds
    abstract fun bindMemberMapper(mapper: MemberMapperImpl): MemberMapper

    @Binds
    abstract fun bindMessageAttachmentMapper(mapper: MessageAttachmentMapperImpl): MessageAttachmentMapper

    @Binds
    abstract fun bindMessageMapper(mapper: MessageMapperImpl): MessageMapper

    @Binds
    abstract fun bindPermissionMapper(mapper: PermissionMapperImpl): PermissionMapper

    @Binds
    abstract fun bindProjectChannelMapper(mapper: ProjectChannelMapperImpl): ProjectChannelMapper

    @Binds
    abstract fun bindProjectInvitationMapper(mapper: ProjectInvitationMapperImpl): ProjectInvitationMapper

    @Binds
    abstract fun bindProjectMapper(mapper: ProjectMapperImpl): ProjectMapper

    @Binds
    abstract fun bindProjectsWrapperMapper(mapper: ProjectsWrapperMapperImpl): ProjectsWrapperMapper

    @Binds
    abstract fun bindReactionMapper(mapper: ReactionMapperImpl): ReactionMapper

    @Binds
    abstract fun bindRoleMapper(mapper: RoleMapperImpl): RoleMapper

    @Binds
    abstract fun bindScheduleMapper(mapper: ScheduleMapperImpl): ScheduleMapper

    @Binds
    abstract fun bindTaskMapper(mapper: TaskMapperImpl): TaskMapper

    @Binds
    abstract fun bindUserMapper(mapper: UserMapperImpl): UserMapper
}
