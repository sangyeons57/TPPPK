package com.example.data_core.di

import com.example.data_core.datasource.remote.DefaultDatasource
import com.example.data_core.datasource.remote.DefaultDatasourceImpl
import com.example.data_core.model.remote.DMChannelDTO
import com.example.data_core.model.remote.DMWrapperDTO
import com.example.data_core.model.remote.FriendDTO
import com.example.data_core.model.remote.MemberDTO
import com.example.data_core.model.remote.MessageAttachmentDTO
import com.example.data_core.model.remote.MessageDTO
import com.example.data_core.model.remote.PermissionDTO
import com.example.data_core.model.remote.ProjectChannelDTO
import com.example.data_core.model.remote.ProjectDTO
import com.example.data_core.model.remote.ProjectInvitationDTO
import com.example.data_core.model.remote.ProjectsWrapperDTO
import com.example.data_core.model.remote.ReactionDTO
import com.example.data_core.model.remote.RoleDTO
import com.example.data_core.model.remote.ScheduleDTO
import com.example.data_core.model.remote.TaskDTO
import com.example.data_core.model.remote.UserDTO
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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    @Named("user")
    fun provideUserDataSource(firestore: FirebaseFirestore): DefaultDatasource<User, UserDTO> {
        return object : DefaultDatasourceImpl<User, UserDTO>(firestore, UserDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("project")
    fun provideProjectDataSource(firestore: FirebaseFirestore): DefaultDatasource<Project, ProjectDTO> {
        return object :
            DefaultDatasourceImpl<Project, ProjectDTO>(firestore, ProjectDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("category")
    fun provideCategoryDataSource(firestore: FirebaseFirestore): DefaultDatasource<Category, CategoryDTO> {
        return object :
            DefaultDatasourceImpl<Category, CategoryDTO>(firestore, CategoryDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("dmChannel")
    fun provideDMChannelDataSource(firestore: FirebaseFirestore): DefaultDatasource<DMChannel, DMChannelDTO> {
        return object :
            DefaultDatasourceImpl<DMChannel, DMChannelDTO>(firestore, DMChannelDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("dmWrapper")
    fun provideDMWrapperDataSource(firestore: FirebaseFirestore): DefaultDatasource<DMWrapper, DMWrapperDTO> {
        return object :
            DefaultDatasourceImpl<DMWrapper, DMWrapperDTO>(firestore, DMWrapperDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("friend")
    fun provideFriendDataSource(firestore: FirebaseFirestore): DefaultDatasource<Friend, FriendDTO> {
        return object :
            DefaultDatasourceImpl<Friend, FriendDTO>(firestore, FriendDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("member")
    fun provideMemberDataSource(firestore: FirebaseFirestore): DefaultDatasource<Member, MemberDTO> {
        return object :
            DefaultDatasourceImpl<Member, MemberDTO>(firestore, MemberDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("message")
    fun provideMessageDataSource(firestore: FirebaseFirestore): DefaultDatasource<Message, MessageDTO> {
        return object :
            DefaultDatasourceImpl<Message, MessageDTO>(firestore, MessageDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("messageAttachment")
    fun provideMessageAttachmentDataSource(firestore: FirebaseFirestore): DefaultDatasource<MessageAttachment, MessageAttachmentDTO> {
        return object : DefaultDatasourceImpl<MessageAttachment, MessageAttachmentDTO>(
            firestore,
            MessageAttachmentDTO::class.java
        ) {}
    }

    @Provides
    @Singleton
    @Named("permission")
    fun providePermissionDataSource(firestore: FirebaseFirestore): DefaultDatasource<Permission, PermissionDTO> {
        return object : DefaultDatasourceImpl<Permission, PermissionDTO>(
            firestore,
            PermissionDTO::class.java
        ) {}
    }

    @Provides
    @Singleton
    @Named("projectChannel")
    fun provideProjectChannelDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectChannel, ProjectChannelDTO> {
        return object : DefaultDatasourceImpl<ProjectChannel, ProjectChannelDTO>(
            firestore,
            ProjectChannelDTO::class.java
        ) {}
    }

    @Provides
    @Singleton
    @Named("projectInvitation")
    fun provideProjectInvitationDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectInvitation, ProjectInvitationDTO> {
        return object : DefaultDatasourceImpl<ProjectInvitation, ProjectInvitationDTO>(
            firestore,
            ProjectInvitationDTO::class.java
        ) {}
    }

    @Provides
    @Singleton
    @Named("projectsWrapper")
    fun provideProjectsWrapperDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectsWrapper, ProjectsWrapperDTO> {
        return object : DefaultDatasourceImpl<ProjectsWrapper, ProjectsWrapperDTO>(
            firestore,
            ProjectsWrapperDTO::class.java
        ) {}
    }

    @Provides
    @Singleton
    @Named("reaction")
    fun provideReactionDataSource(firestore: FirebaseFirestore): DefaultDatasource<Reaction, ReactionDTO> {
        return object :
            DefaultDatasourceImpl<Reaction, ReactionDTO>(firestore, ReactionDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("role")
    fun provideRoleDataSource(firestore: FirebaseFirestore): DefaultDatasource<Role, RoleDTO> {
        return object : DefaultDatasourceImpl<Role, RoleDTO>(firestore, RoleDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("schedule")
    fun provideScheduleDataSource(firestore: FirebaseFirestore): DefaultDatasource<Schedule, ScheduleDTO> {
        return object :
            DefaultDatasourceImpl<Schedule, ScheduleDTO>(firestore, ScheduleDTO::class.java) {}
    }

    @Provides
    @Singleton
    @Named("task")
    fun provideTaskDataSource(firestore: FirebaseFirestore): DefaultDatasource<Task, TaskDTO> {
        return object : DefaultDatasourceImpl<Task, TaskDTO>(firestore, TaskDTO::class.java) {}
    }
}