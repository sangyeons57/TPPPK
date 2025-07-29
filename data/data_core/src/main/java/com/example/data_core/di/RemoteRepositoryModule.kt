package com.example.data_core.di

import com.example.data_core.datasource.remote.DefaultDatasource
import com.example.data_core.datasource.remote.DefaultDatasourceImpl
import com.example.data_core.model.remote.*
import com.example.data_core.repository.remote.DefaultRepositoryImpl
import com.example.data_model.remote.CategoryDTO
import com.example.domain.model.base.*
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.remote.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteRepositoryModule {

    // === Generic DataSource Providers ===

    @Provides
    @Named("categories")
    fun provideCategoriesDataSource(firestore: FirebaseFirestore): DefaultDatasource<Category, CategoryDTO> =
        DefaultDatasourceImpl<Category, CategoryDTO>(firestore, CategoryDTO::class.java)

    @Provides
    @Named("users")
    fun provideUsersDataSource(firestore: FirebaseFirestore): DefaultDatasource<User, UserDTO> =
        DefaultDatasourceImpl<User, UserDTO>(firestore, UserDTO::class.java)

    @Provides
    @Named("projects")
    fun provideProjectsDataSource(firestore: FirebaseFirestore): DefaultDatasource<Project, ProjectDTO> =
        DefaultDatasourceImpl<Project, ProjectDTO>(firestore, ProjectDTO::class.java)
            .setCollection(CollectionPath("projects"))

    @Provides
    @Named("projectChannels")
    fun provideProjectChannelsDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectChannel, ProjectChannelDTO> =
        DefaultDatasourceImpl<ProjectChannel, ProjectChannelDTO>(firestore, ProjectChannelDTO::class.java)
            .setCollection(CollectionPath("project_channels"))

    @Provides
    @Named("messages")
    fun provideMessagesDataSource(firestore: FirebaseFirestore): DefaultDatasource<Message, MessageDTO> =
        DefaultDatasourceImpl<Message, MessageDTO>(firestore, MessageDTO::class.java)
            .setCollection(CollectionPath("messages"))

    @Provides
    @Named("messageAttachments")
    fun provideMessageAttachmentsDataSource(firestore: FirebaseFirestore): DefaultDatasource<MessageAttachment, MessageAttachmentDTO> =
        DefaultDatasourceImpl<MessageAttachment, MessageAttachmentDTO>(firestore, MessageAttachmentDTO::class.java)
            .setCollection(CollectionPath("message_attachments"))

    @Provides
    @Named("dmChannels")
    fun provideDmChannelsDataSource(firestore: FirebaseFirestore): DefaultDatasource<DMChannel, DMChannelDTO> =
        DefaultDatasourceImpl<DMChannel, DMChannelDTO>(firestore, DMChannelDTO::class.java)
            .setCollection(CollectionPath("dm_channels"))

    @Provides
    @Named("dmWrapper")
    fun provideDmWrapperDataSource(firestore: FirebaseFirestore): DefaultDatasource<DMWrapper, DMWrapperDTO> =
        DefaultDatasourceImpl<DMWrapper, DMWrapperDTO>(firestore, DMWrapperDTO::class.java)
            .setCollection(CollectionPath("dm_wrapper"))

    @Provides
    @Named("friends")
    fun provideFriendsDataSource(firestore: FirebaseFirestore): DefaultDatasource<Friend, FriendDTO> =
        DefaultDatasourceImpl<Friend, FriendDTO>(firestore, FriendDTO::class.java)
            .setCollection(CollectionPath("friends"))

    @Provides
    @Named("members")
    fun provideMembersDataSource(firestore: FirebaseFirestore): DefaultDatasource<Member, MemberDTO> =
        DefaultDatasourceImpl<Member, MemberDTO>(firestore, MemberDTO::class.java)
            .setCollection(CollectionPath("members"))

    @Provides
    @Named("permissions")
    fun providePermissionsDataSource(firestore: FirebaseFirestore): DefaultDatasource<Permission, PermissionDTO> =
        DefaultDatasourceImpl<Permission, PermissionDTO>(firestore, PermissionDTO::class.java)
            .setCollection(CollectionPath("permissions"))

    @Provides
    @Named("projectInvitations")
    fun provideProjectInvitationsDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectInvitation, ProjectInvitationDTO> =
        DefaultDatasourceImpl<ProjectInvitation, ProjectInvitationDTO>(firestore, ProjectInvitationDTO::class.java)
            .setCollection(CollectionPath("project_invitations"))

    @Provides
    @Named("roles")
    fun provideRolesDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectRole, RoleDTO> =
        DefaultDatasourceImpl<ProjectRole, RoleDTO>(firestore, RoleDTO::class.java)
            .setCollection(CollectionPath("roles"))

    @Provides
    @Named("projectsWrapper")
    fun provideProjectsWrapperDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectsWrapper, ProjectsWrapperDTO> =
        DefaultDatasourceImpl<ProjectsWrapper, ProjectsWrapperDTO>(firestore, ProjectsWrapperDTO::class.java)
            .setCollection(CollectionPath("projects_wrapper"))

    @Provides
    @Named("schedules")
    fun provideSchedulesDataSource(firestore: FirebaseFirestore): DefaultDatasource<Schedule, ScheduleDTO> =
        DefaultDatasourceImpl<Schedule, ScheduleDTO>(firestore, ScheduleDTO::class.java)
            .setCollection(CollectionPath("schedules"))

    @Provides
    @Named("tasks")
    fun provideTasksDataSource(firestore: FirebaseFirestore): DefaultDatasource<Task, TaskDTO> =
        DefaultDatasourceImpl<Task, TaskDTO>(firestore, TaskDTO::class.java)
            .setCollection(CollectionPath("tasks"))

    // === Generic Repository Providers ===

    @Provides
    @Singleton
    fun provideCategoryRepository(@Named("categories") dataSource: DefaultDatasource<Category, CategoryDTO>): CategoryRepository =
        DefaultRepositoryImpl<Category, CategoryDTO>(dataSource, Category::class.java)

    // Note: UserRepository, ProjectRepository, ProjectChannelRepository, MessageRepository 
    // have custom Firebase Functions logic and will be provided separately

    @Provides
    @Singleton
    fun provideMessageAttachmentRepository(@Named("messageAttachments") dataSource: DefaultDatasource<MessageAttachment, MessageAttachmentDTO>): MessageAttachmentRepository =
        DefaultRepositoryImpl<MessageAttachment, MessageAttachmentDTO>(dataSource, MessageAttachment::class.java)

    @Provides
    @Singleton
    fun provideDMChannelRepository(@Named("dmChannels") dataSource: DefaultDatasource<DMChannel, DMChannelDTO>): DMChannelRepository =
        DefaultRepositoryImpl<DMChannel, DMChannelDTO>(dataSource, DMChannel::class.java)

    @Provides
    @Singleton
    fun provideDMWrapperRepository(@Named("dmWrapper") dataSource: DefaultDatasource<DMWrapper, DMWrapperDTO>): DMWrapperRepository =
        DefaultRepositoryImpl<DMWrapper, DMWrapperDTO>(dataSource, DMWrapper::class.java)

    @Provides
    @Singleton
    fun provideFriendRepository(@Named("friends") dataSource: DefaultDatasource<Friend, FriendDTO>): FriendRepository =
        DefaultRepositoryImpl<Friend, FriendDTO>(dataSource, Friend::class.java)

    @Provides
    @Singleton
    fun provideMemberRepository(@Named("members") dataSource: DefaultDatasource<Member, MemberDTO>): MemberRepository =
        DefaultRepositoryImpl<Member, MemberDTO>(dataSource, Member::class.java)

    @Provides
    @Singleton
    fun providePermissionRepository(@Named("permissions") dataSource: DefaultDatasource<Permission, PermissionDTO>): PermissionRepository =
        DefaultRepositoryImpl<Permission, PermissionDTO>(dataSource, Permission::class.java)

    @Provides
    @Singleton
    fun provideProjectInvitationRepository(@Named("projectInvitations") dataSource: DefaultDatasource<ProjectInvitation, ProjectInvitationDTO>): ProjectInvitationRepository =
        DefaultRepositoryImpl<ProjectInvitation, ProjectInvitationDTO>(dataSource, ProjectInvitation::class.java)

    @Provides
    @Singleton
    fun provideProjectRoleRepository(@Named("roles") dataSource: DefaultDatasource<ProjectRole, RoleDTO>): ProjectRoleRepository =
        DefaultRepositoryImpl<ProjectRole, RoleDTO>(dataSource, ProjectRole::class.java)

    @Provides
    @Singleton
    fun provideProjectsWrapperRepository(@Named("projectsWrapper") dataSource: DefaultDatasource<ProjectsWrapper, ProjectsWrapperDTO>): ProjectsWrapperRepository =
        DefaultRepositoryImpl<ProjectsWrapper, ProjectsWrapperDTO>(dataSource, ProjectsWrapper::class.java)

    @Provides
    @Singleton
    fun provideScheduleRepository(@Named("schedules") dataSource: DefaultDatasource<Schedule, ScheduleDTO>): ScheduleRepository =
        DefaultRepositoryImpl<Schedule, ScheduleDTO>(dataSource, Schedule::class.java)

    @Provides
    @Singleton
    fun provideTaskRepository(@Named("tasks") dataSource: DefaultDatasource<Task, TaskDTO>): TaskRepository =
        DefaultRepositoryImpl<Task, TaskDTO>(dataSource, Task::class.java)
}