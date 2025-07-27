package com.example.mapper.di

import com.example.data.model.remote.DMChannelDTO
import com.example.data.model.remote.DMWrapperDTO
import com.example.data.model.remote.FriendDTO
import com.example.data.model.remote.MemberDTO
import com.example.data.model.remote.MessageAttachmentDTO
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.PermissionDTO
import com.example.data.model.remote.ProjectChannelDTO
import com.example.data.model.remote.ProjectDTO
import com.example.data.model.remote.ProjectInvitationDTO
import com.example.data.model.remote.ProjectsWrapperDTO
import com.example.data.model.remote.ReactionDTO
import com.example.data.model.remote.RoleDTO
import com.example.data.model.remote.ScheduleDTO
import com.example.data.model.remote.TaskDTO
import com.example.data.model.remote.UserDTO
import com.example.data_model.remote.CategoryDTO
import com.example.domain.model.base.Category
import com.example.domain.model.base.DMChannel
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Friend
import com.example.domain.model.base.Member
import com.example.domain.model.base.Message
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.base.Permission
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.Reaction
import com.example.domain.model.base.Role
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.Task
import com.example.domain.model.base.User
import com.example.mapper.CategoryMapper
import com.example.mapper.DMChannelMapper
import com.example.mapper.DMWrapperMapper
import com.example.mapper.FriendMapper
import com.example.mapper.MemberMapper
import com.example.mapper.MessageAttachmentMapper
import com.example.mapper.MessageMapper
import com.example.mapper.PermissionMapper
import com.example.mapper.ProjectChannelMapper
import com.example.mapper.ProjectInvitationMapper
import com.example.mapper.ProjectMapper
import com.example.mapper.ProjectsWrapperMapper
import com.example.mapper.ReactionMapper
import com.example.mapper.RoleMapper
import com.example.mapper.ScheduleMapper
import com.example.mapper.TaskMapper
import com.example.mapper.UserMapper
import com.example.mapper.base.BaseMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BaseMapperModule {

    @Binds
    abstract fun bindCategoryBaseMapper(mapper: CategoryMapper): BaseMapper<Category, CategoryDTO>

    @Binds
    abstract fun bindDMChannelBaseMapper(mapper: DMChannelMapper): BaseMapper<DMChannel, DMChannelDTO>

    @Binds
    abstract fun bindDMWrapperBaseMapper(mapper: DMWrapperMapper): BaseMapper<DMWrapper, DMWrapperDTO>

    @Binds
    abstract fun bindFriendBaseMapper(mapper: FriendMapper): BaseMapper<Friend, FriendDTO>

    @Binds
    abstract fun bindMemberBaseMapper(mapper: MemberMapper): BaseMapper<Member, MemberDTO>

    @Binds
    abstract fun bindMessageAttachmentBaseMapper(mapper: MessageAttachmentMapper): BaseMapper<MessageAttachment, MessageAttachmentDTO>

    @Binds
    abstract fun bindMessageBaseMapper(mapper: MessageMapper): BaseMapper<Message, MessageDTO>

    @Binds
    abstract fun bindPermissionBaseMapper(mapper: PermissionMapper): BaseMapper<Permission, PermissionDTO>

    @Binds
    abstract fun bindProjectChannelBaseMapper(mapper: ProjectChannelMapper): BaseMapper<ProjectChannel, ProjectChannelDTO>

    @Binds
    abstract fun bindProjectInvitationBaseMapper(mapper: ProjectInvitationMapper): BaseMapper<ProjectInvitation, ProjectInvitationDTO>

    @Binds
    abstract fun bindProjectBaseMapper(mapper: ProjectMapper): BaseMapper<Project, ProjectDTO>

    @Binds
    abstract fun bindProjectsWrapperBaseMapper(mapper: ProjectsWrapperMapper): BaseMapper<ProjectsWrapper, ProjectsWrapperDTO>

    @Binds
    abstract fun bindReactionBaseMapper(mapper: ReactionMapper): BaseMapper<Reaction, ReactionDTO>

    @Binds
    abstract fun bindRoleBaseMapper(mapper: RoleMapper): BaseMapper<Role, RoleDTO>

    @Binds
    abstract fun bindScheduleBaseMapper(mapper: ScheduleMapper): BaseMapper<Schedule, ScheduleDTO>

    @Binds
    abstract fun bindTaskBaseMapper(mapper: TaskMapper): BaseMapper<Task, TaskDTO>

    @Binds
    abstract fun bindUserBaseMapper(mapper: UserMapper): BaseMapper<User, UserDTO>
}
