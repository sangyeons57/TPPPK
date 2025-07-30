package com.example.mapper.di

import com.example.data_model.local.MessageEntity
import com.example.data_model.remote.CategoryDTO
import com.example.data_model.remote.DMChannelDTO
import com.example.data_model.remote.DMWrapperDTO
import com.example.data_model.remote.FriendDTO
import com.example.data_model.remote.MemberDTO
import com.example.data_model.remote.MessageAttachmentDTO
import com.example.data_model.remote.MessageDTO
import com.example.data_model.remote.PermissionDTO
import com.example.data_model.remote.ProjectChannelDTO
import com.example.data_model.remote.ProjectDTO
import com.example.data_model.remote.ProjectInvitationDTO
import com.example.data_model.remote.ProjectsWrapperDTO
import com.example.data_model.remote.ReactionDTO
import com.example.data_model.remote.RoleDTO
import com.example.data_model.remote.ScheduleDTO
import com.example.data_model.remote.TaskDTO
import com.example.data_model.remote.UserDTO
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
import com.example.mapper.DtoMapper
import com.example.mapper.Mapper
import com.example.mapper.category.CategoryMapper
import com.example.mapper.dm.DMChannelMapper
import com.example.mapper.dm.DMWrapperMapper
import com.example.mapper.friend.FriendMapper
import com.example.mapper.member.MemberMapper
import com.example.mapper.message.MessageAttachmentMapper
import com.example.mapper.message.MessageMapper
import com.example.mapper.permission.PermissionMapper
import com.example.mapper.project.ProjectChannelMapper
import com.example.mapper.project.ProjectInvitationMapper
import com.example.mapper.project.ProjectMapper
import com.example.mapper.project.ProjectsWrapperMapper
import com.example.mapper.reaction.ReactionMapper
import com.example.mapper.role.RoleMapper
import com.example.mapper.schedule.ScheduleMapper
import com.example.mapper.sync.OutBoxMapper
import com.example.mapper.task.TaskMapper
import com.example.mapper.user.UserMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mapper들을 DI 컨테이너에 제공하는 모듈
 * Clean Architecture 원칙에 따라 JSON 의존성 없는 순수한 매핑만 담당
 */
@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    // Entity가 있는 Mapper
    @Provides
    @Singleton
    fun provideMessageMapper(): MessageMapper = MessageMapper()

    @Provides
    @Singleton
    fun provideOutBoxMapper(): OutBoxMapper = OutBoxMapper()

    // Entity가 없는 DtoMapper들
    @Provides
    @Singleton
    fun provideUserMapper(): UserMapper = UserMapper()

    @Provides
    @Singleton
    fun provideProjectMapper(): ProjectMapper = ProjectMapper()

    @Provides
    @Singleton
    fun provideProjectInvitationMapper(): ProjectInvitationMapper = ProjectInvitationMapper()

    @Provides
    @Singleton
    fun provideCategoryMapper(): CategoryMapper = CategoryMapper()

    @Provides
    @Singleton
    fun provideTaskMapper(): TaskMapper = TaskMapper()

    @Provides
    @Singleton
    fun provideScheduleMapper(): ScheduleMapper = ScheduleMapper()

    @Provides
    @Singleton
    fun provideMemberMapper(): MemberMapper = MemberMapper()

    @Provides
    @Singleton
    fun provideFriendMapper(): FriendMapper = FriendMapper()

    @Provides
    @Singleton
    fun provideProjectChannelMapper(): ProjectChannelMapper = ProjectChannelMapper()

    @Provides
    @Singleton
    fun provideDMWrapperMapper(): DMWrapperMapper = DMWrapperMapper()

    @Provides
    @Singleton
    fun provideDMChannelMapper(): DMChannelMapper = DMChannelMapper()

    @Provides
    @Singleton
    fun provideReactionMapper(): ReactionMapper = ReactionMapper()

    @Provides
    @Singleton
    fun providePermissionMapper(): PermissionMapper = PermissionMapper()

    @Provides
    @Singleton
    fun provideRoleMapper(): RoleMapper = RoleMapper()

    @Provides
    @Singleton
    fun provideProjectsWrapperMapper(): ProjectsWrapperMapper = ProjectsWrapperMapper()

    @Provides
    @Singleton
    fun provideMessageAttachmentMapper(): MessageAttachmentMapper = MessageAttachmentMapper()

    // ================================
    // Generic Mapper Interfaces
    // ================================

    // DtoMapper 제네릭 인터페이스들
    @Provides
    @Singleton
    fun provideUserDtoMapper(mapper: UserMapper): DtoMapper<User, UserDTO> = mapper

    @Provides
    @Singleton
    fun provideProjectDtoMapper(mapper: ProjectMapper): DtoMapper<Project, ProjectDTO> = mapper

    @Provides
    @Singleton
    fun provideProjectInvitationDtoMapper(mapper: ProjectInvitationMapper): DtoMapper<ProjectInvitation, ProjectInvitationDTO> =
        mapper

    @Provides
    @Singleton
    fun provideCategoryDtoMapper(mapper: CategoryMapper): DtoMapper<Category, CategoryDTO> = mapper

    @Provides
    @Singleton
    fun provideTaskDtoMapper(mapper: TaskMapper): DtoMapper<Task, TaskDTO> = mapper

    @Provides
    @Singleton
    fun provideScheduleDtoMapper(mapper: ScheduleMapper): DtoMapper<Schedule, ScheduleDTO> = mapper

    @Provides
    @Singleton
    fun provideMemberDtoMapper(mapper: MemberMapper): DtoMapper<Member, MemberDTO> = mapper

    @Provides
    @Singleton
    fun provideFriendDtoMapper(mapper: FriendMapper): DtoMapper<Friend, FriendDTO> = mapper

    @Provides
    @Singleton
    fun provideProjectChannelDtoMapper(mapper: ProjectChannelMapper): DtoMapper<ProjectChannel, ProjectChannelDTO> =
        mapper

    @Provides
    @Singleton
    fun provideDMWrapperDtoMapper(mapper: DMWrapperMapper): DtoMapper<DMWrapper, DMWrapperDTO> =
        mapper

    @Provides
    @Singleton
    fun provideDMChannelDtoMapper(mapper: DMChannelMapper): DtoMapper<DMChannel, DMChannelDTO> =
        mapper

    @Provides
    @Singleton
    fun provideReactionDtoMapper(mapper: ReactionMapper): DtoMapper<Reaction, ReactionDTO> = mapper

    @Provides
    @Singleton
    fun providePermissionDtoMapper(mapper: PermissionMapper): DtoMapper<Permission, PermissionDTO> =
        mapper

    @Provides
    @Singleton
    fun provideRoleDtoMapper(mapper: RoleMapper): DtoMapper<Role, RoleDTO> = mapper

    @Provides
    @Singleton
    fun provideProjectsWrapperDtoMapper(mapper: ProjectsWrapperMapper): DtoMapper<ProjectsWrapper, ProjectsWrapperDTO> =
        mapper

    @Provides
    @Singleton
    fun provideMessageAttachmentDtoMapper(mapper: MessageAttachmentMapper): DtoMapper<MessageAttachment, MessageAttachmentDTO> =
        mapper

    // Full Mapper 제네릭 인터페이스들 (Entity + Domain + DTO)
    @Provides
    @Singleton
    fun provideMessageFullMapper(mapper: MessageMapper): Mapper<MessageEntity, Message, MessageDTO> =
        mapper
}