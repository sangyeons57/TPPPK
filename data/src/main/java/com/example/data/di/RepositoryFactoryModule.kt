package com.example.data.di

import com.example.data.repository.factory.AuthRepositoryFactoryImpl
import com.example.data.repository.factory.CategoryRepositoryFactoryImpl
import com.example.data.repository.factory.DMChannelRepositoryFactoryImpl
import com.example.data.repository.factory.DMWrapperRepositoryFactoryImpl
import com.example.data.repository.factory.FileRepositoryFactoryImpl
import com.example.data.repository.factory.FriendRepositoryFactoryImpl
import com.example.data.repository.factory.MediaRepositoryFactoryImpl
import com.example.data.repository.factory.MemberRepositoryFactoryImpl
import com.example.data.repository.factory.MessageAttachmentRepositoryFactoryImpl
import com.example.data.repository.factory.MessageRepositoryFactoryImpl
import com.example.data.repository.factory.PermissionRepositoryFactoryImpl
import com.example.data.repository.factory.ProjectChannelRepositoryFactoryImpl
import com.example.data.repository.factory.ProjectRepositoryFactoryImpl
import com.example.data.repository.factory.ProjectInvitationRepositoryFactoryImpl
import com.example.data.repository.base.ProjectInvitationRepositoryImpl
import com.example.data.repository.factory.ProjectsWrapperRepositoryFactoryImpl
import com.example.data.repository.factory.RoleRepositoryFactoryImpl
import com.example.data.repository.factory.ScheduleRepositoryFactoryImpl
import com.example.data.repository.factory.SearchRepositoryFactoryImpl
import com.example.data.repository.factory.TaskContainerRepositoryFactoryImpl
import com.example.data.repository.factory.TaskRepositoryFactoryImpl
import com.example.data.repository.factory.UserRepositoryFactoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.base.DMWrapperRepository
import com.example.domain.repository.base.FileRepository
import com.example.domain.repository.base.FriendRepository
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.MessageAttachmentRepository
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.base.PermissionRepository
import com.example.domain.repository.base.ProjectChannelRepository
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectInvitationRepository
import com.example.domain.repository.base.ProjectRoleRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.base.ScheduleRepository
import com.example.domain.repository.base.SearchRepository
import com.example.domain.repository.base.TaskContainerRepository
import com.example.domain.repository.base.TaskRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
import com.example.domain.repository.factory.context.FileRepositoryFactoryContext
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
import com.example.domain.repository.factory.context.MediaRepositoryFactoryContext
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import com.example.domain.repository.factory.context.MessageAttachmentRepositoryFactoryContext
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import com.example.domain.repository.factory.context.ScheduleRepositoryFactoryContext
import com.example.domain.repository.factory.context.SearchRepositoryFactoryContext
import com.example.domain.repository.factory.context.TaskContainerRepositoryFactoryContext
import com.example.domain.repository.factory.context.TaskRepositoryFactoryContext
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryFactoryModule {

    @Binds
    @Singleton
        abstract fun bindUserRepositoryFactory(impl: UserRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>

    @Binds
    @Singleton
        abstract fun bindAuthRepositoryFactory(impl: AuthRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>

    @Binds
    @Singleton
        abstract fun bindCategoryRepositoryFactory(impl: CategoryRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>

    @Binds
    @Singleton
        abstract fun bindDMChannelRepositoryFactory(impl: DMChannelRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<DMChannelRepositoryFactoryContext, DMChannelRepository>

    @Binds
    @Singleton
        abstract fun bindDMWrapperRepositoryFactory(impl: DMWrapperRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<DMWrapperRepositoryFactoryContext, DMWrapperRepository>

    @Binds
    @Singleton
        abstract fun bindFileRepositoryFactory(impl: FileRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<FileRepositoryFactoryContext, FileRepository>

    @Binds
    @Singleton
        abstract fun bindFriendRepositoryFactory(impl: FriendRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<FriendRepositoryFactoryContext, FriendRepository>


    @Binds
    @Singleton
        abstract fun bindMediaRepositoryFactory(impl: MediaRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<MediaRepositoryFactoryContext, MediaRepository>

    @Binds
    @Singleton
        abstract fun bindMemberRepositoryFactory(impl: MemberRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<MemberRepositoryFactoryContext, MemberRepository>

    @Binds
    @Singleton
        abstract fun bindMessageAttachmentRepositoryFactory(impl: MessageAttachmentRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<MessageAttachmentRepositoryFactoryContext, MessageAttachmentRepository>

    @Binds
    @Singleton
        abstract fun bindMessageRepositoryFactory(impl: MessageRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<MessageRepositoryFactoryContext, MessageRepository>

    @Binds
    @Singleton
        abstract fun bindPermissionRepositoryFactory(impl: PermissionRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<PermissionRepositoryFactoryContext, PermissionRepository>

    @Binds
    @Singleton
        abstract fun bindProjectChannelRepositoryFactory(impl: ProjectChannelRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<ProjectChannelRepositoryFactoryContext, ProjectChannelRepository>

    @Binds
    @Singleton
        abstract fun bindProjectRepositoryFactory(impl: ProjectRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<ProjectRepositoryFactoryContext, ProjectRepository>

    @Binds
    @Singleton
        abstract fun bindProjectInvitationRepositoryFactory(impl: ProjectInvitationRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<ProjectInvitationRepositoryFactoryContext, ProjectInvitationRepository>

    @Binds
    @Singleton
        abstract fun bindProjectInvitationRepository(impl: ProjectInvitationRepositoryImpl): ProjectInvitationRepository

    @Binds
    @Singleton
        abstract fun bindProjectsWrapperRepositoryFactory(impl: ProjectsWrapperRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<ProjectsWrapperRepositoryFactoryContext, ProjectsWrapperRepository>


    @Binds
    @Singleton
        abstract fun bindRoleRepositoryFactory(impl: RoleRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<ProjectRoleRepositoryFactoryContext, ProjectRoleRepository>

    @Binds
    @Singleton
        abstract fun bindScheduleRepositoryFactory(impl: ScheduleRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<ScheduleRepositoryFactoryContext, ScheduleRepository>

    @Binds
    @Singleton
    abstract fun bindSearchRepositoryFactory(impl: SearchRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<SearchRepositoryFactoryContext, SearchRepository>

    @Binds
    @Singleton
    abstract fun bindTaskContainerRepositoryFactory(impl: TaskContainerRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<TaskContainerRepositoryFactoryContext, TaskContainerRepository>

    @Binds
    @Singleton
    abstract fun bindTaskRepositoryFactory(impl: TaskRepositoryFactoryImpl): @JvmSuppressWildcards RepositoryFactory<TaskRepositoryFactoryContext, TaskRepository>

}