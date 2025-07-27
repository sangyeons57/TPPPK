package com.example.data.di

import com.example.data.datasource.remote.DefaultDatasource
import com.example.data.datasource.remote.DefaultDatasourceImpl
import com.example.data.model.remote.*
import com.example.data.repository.DefaultRepositoryImpl
import com.example.data_model.remote.CategoryDTO
import com.example.domain.model.base.*
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.base.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // === Generic DataSource Providers ===

    @Provides
    @Named("categories")
    fun provideCategoriesDataSource(firestore: FirebaseFirestore): DefaultDatasource<Category, CategoryDTO> =
        DefaultDatasourceImpl(firestore, CategoryDTO::class.java)
            .setCollection(CollectionPath("categories"))

    @Provides
    @Named("users")
    fun provideUsersDataSource(firestore: FirebaseFirestore): DefaultDatasource<UserDTO> =
        DefaultDatasourceImpl(firestore, UserDTO::class.java)
            .setCollection(CollectionPath("users"))

    @Provides
    @Named("projects")
    fun provideProjectsDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectDTO> =
        DefaultDatasourceImpl(firestore, ProjectDTO::class.java)
            .setCollection(CollectionPath("projects"))

    @Provides
    @Named("projectChannels")
    fun provideProjectChannelsDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectChannelDTO> =
        DefaultDatasourceImpl(firestore, ProjectChannelDTO::class.java)
            .setCollection(CollectionPath("project_channels"))

    @Provides
    @Named("messages")
    fun provideMessagesDataSource(firestore: FirebaseFirestore): DefaultDatasource<MessageDTO> =
        DefaultDatasourceImpl(firestore, MessageDTO::class.java)
            .setCollection(CollectionPath("messages"))

    @Provides
    @Named("messageAttachments")
    fun provideMessageAttachmentsDataSource(firestore: FirebaseFirestore): DefaultDatasource<MessageAttachmentDTO> =
        DefaultDatasourceImpl(firestore, MessageAttachmentDTO::class.java)
            .setCollection(CollectionPath("message_attachments"))

    @Provides
    @Named("dmChannels")
    fun provideDmChannelsDataSource(firestore: FirebaseFirestore): DefaultDatasource<DMChannelDTO> =
        DefaultDatasourceImpl(firestore, DMChannelDTO::class.java)
            .setCollection(CollectionPath("dm_channels"))

    @Provides
    @Named("dmWrapper")
    fun provideDmWrapperDataSource(firestore: FirebaseFirestore): DefaultDatasource<DMWrapperDTO> =
        DefaultDatasourceImpl(firestore, DMWrapperDTO::class.java)
            .setCollection(CollectionPath("dm_wrapper"))

    @Provides
    @Named("friends")
    fun provideFriendsDataSource(firestore: FirebaseFirestore): DefaultDatasource<FriendDTO> =
        DefaultDatasourceImpl(firestore, FriendDTO::class.java)
            .setCollection(CollectionPath("friends"))

    @Provides
    @Named("members")
    fun provideMembersDataSource(firestore: FirebaseFirestore): DefaultDatasource<MemberDTO> =
        DefaultDatasourceImpl(firestore, MemberDTO::class.java)
            .setCollection(CollectionPath("members"))

    @Provides
    @Named("permissions")
    fun providePermissionsDataSource(firestore: FirebaseFirestore): DefaultDatasource<PermissionDTO> =
        DefaultDatasourceImpl(firestore, PermissionDTO::class.java)
            .setCollection(CollectionPath("permissions"))

    @Provides
    @Named("projectInvitations")
    fun provideProjectInvitationsDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectInvitationDTO> =
        DefaultDatasourceImpl(firestore, ProjectInvitationDTO::class.java)
            .setCollection(CollectionPath("project_invitations"))

    @Provides
    @Named("roles")
    fun provideRolesDataSource(firestore: FirebaseFirestore): DefaultDatasource<RoleDTO> =
        DefaultDatasourceImpl(firestore, RoleDTO::class.java)
            .setCollection(CollectionPath("roles"))

    @Provides
    @Named("projectsWrapper")
    fun provideProjectsWrapperDataSource(firestore: FirebaseFirestore): DefaultDatasource<ProjectsWrapperDTO> =
        DefaultDatasourceImpl(firestore, ProjectsWrapperDTO::class.java)
            .setCollection(CollectionPath("projects_wrapper"))

    @Provides
    @Named("schedules")
    fun provideSchedulesDataSource(firestore: FirebaseFirestore): DefaultDatasource<ScheduleDTO> =
        DefaultDatasourceImpl(firestore, ScheduleDTO::class.java)
            .setCollection(CollectionPath("schedules"))

    @Provides
    @Named("tasks")
    fun provideTasksDataSource(firestore: FirebaseFirestore): DefaultDatasource<TaskDTO> =
        DefaultDatasourceImpl(firestore, TaskDTO::class.java)
            .setCollection(CollectionPath("tasks"))

    // === Generic Repository Providers ===

    @Provides
    @Singleton
    fun provideCategoryRepository(@Named("categories") dataSource: DefaultDatasource<CategoryDTO>): CategoryRepository =
        DefaultRepositoryImpl(dataSource, Category::class.java)

    // Note: UserRepository, ProjectRepository, ProjectChannelRepository, MessageRepository 
    // have custom Firebase Functions logic and will be provided separately

    @Provides
    @Singleton
    fun provideMessageAttachmentRepository(@Named("messageAttachments") dataSource: DefaultDatasource<MessageAttachmentDTO>): MessageAttachmentRepository =
        DefaultRepositoryImpl(dataSource, MessageAttachment::class.java)

    @Provides
    @Singleton
    fun provideDMChannelRepository(@Named("dmChannels") dataSource: DefaultDatasource<DMChannelDTO>): DMChannelRepository =
        DefaultRepositoryImpl(dataSource, DMChannel::class.java)

    @Provides
    @Singleton
    fun provideDMWrapperRepository(@Named("dmWrapper") dataSource: DefaultDatasource<DMWrapperDTO>): DMWrapperRepository =
        DefaultRepositoryImpl(dataSource, DMWrapper::class.java)

    @Provides
    @Singleton
    fun provideFriendRepository(@Named("friends") dataSource: DefaultDatasource<FriendDTO>): FriendRepository =
        DefaultRepositoryImpl(dataSource, Friend::class.java)

    @Provides
    @Singleton
    fun provideMemberRepository(@Named("members") dataSource: DefaultDatasource<MemberDTO>): MemberRepository =
        DefaultRepositoryImpl(dataSource, Member::class.java)

    @Provides
    @Singleton
    fun providePermissionRepository(@Named("permissions") dataSource: DefaultDatasource<PermissionDTO>): PermissionRepository =
        DefaultRepositoryImpl(dataSource, Permission::class.java)

    @Provides
    @Singleton
    fun provideProjectInvitationRepository(@Named("projectInvitations") dataSource: DefaultDatasource<ProjectInvitationDTO>): ProjectInvitationRepository =
        DefaultRepositoryImpl(dataSource, ProjectInvitation::class.java)

    @Provides
    @Singleton
    fun provideProjectRoleRepository(@Named("roles") dataSource: DefaultDatasource<RoleDTO>): ProjectRoleRepository =
        DefaultRepositoryImpl(dataSource, ProjectRole::class.java)

    @Provides
    @Singleton
    fun provideProjectsWrapperRepository(@Named("projectsWrapper") dataSource: DefaultDatasource<ProjectsWrapperDTO>): ProjectsWrapperRepository =
        DefaultRepositoryImpl(dataSource, ProjectsWrapper::class.java)

    @Provides
    @Singleton
    fun provideScheduleRepository(@Named("schedules") dataSource: DefaultDatasource<ScheduleDTO>): ScheduleRepository =
        DefaultRepositoryImpl(dataSource, Schedule::class.java)

    @Provides
    @Singleton
    fun provideTaskRepository(@Named("tasks") dataSource: DefaultDatasource<TaskDTO>): TaskRepository =
        DefaultRepositoryImpl(dataSource, Task::class.java)
}