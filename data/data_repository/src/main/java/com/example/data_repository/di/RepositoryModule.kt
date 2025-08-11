package com.example.data_repository.di

import com.example.data_repository.base.AuthRepositoryImpl
import com.example.data_repository.base.CategoryRepositoryImpl
import com.example.data_repository.base.DMChannelRepositoryImpl
import com.example.data_repository.base.DMWrapperRepositoryImpl
import com.example.data_repository.base.FileRepositoryImpl
import com.example.data_repository.base.FriendRepositoryImpl
import com.example.data_repository.base.MemberRepositoryImpl
import com.example.data_repository.base.MessageRepositoryImpl
import com.example.data_repository.base.PermissionRepositoryImpl
import com.example.data_repository.base.ProjectChannelRepositoryImpl
import com.example.data_repository.base.ProjectInvitationRepositoryImpl
import com.example.data_repository.base.ProjectRepositoryImpl
import com.example.data_repository.base.ProjectRoleRepositoryImpl
import com.example.data_repository.base.ProjectsWrapperRepositoryImpl
import com.example.data_repository.base.ScheduleRepositoryImpl
import com.example.data_repository.base.SearchRepositoryImpl
import com.example.data_repository.base.TaskRepositoryImpl
import com.example.data_repository.base.UserRepositoryImpl
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.CategoryRepository
import com.example.domain_repository.base.DMChannelRepository
import com.example.domain_repository.base.DMWrapperRepository
import com.example.domain_repository.base.FileRepository
import com.example.domain_repository.base.FriendRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.base.PermissionRepository
import com.example.domain_repository.base.ProjectChannelRepository
import com.example.domain_repository.base.ProjectInvitationRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.domain_repository.base.ProjectsWrapperRepository
import com.example.domain_repository.base.ScheduleRepository
import com.example.domain_repository.base.SearchRepository
import com.example.domain_repository.base.TaskRepository
import com.example.domain_repository.base.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 구현체들을 인터페이스에 바인딩하는 Hilt 모듈
 * RepositoryFactory 패턴을 제거하고 직접 DI로 Repository를 제공합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindDMChannelRepository(impl: DMChannelRepositoryImpl): DMChannelRepository

    @Binds
    @Singleton
    abstract fun bindDMWrapperRepository(impl: DMWrapperRepositoryImpl): DMWrapperRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository


    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindProjectChannelRepository(impl: ProjectChannelRepositoryImpl): ProjectChannelRepository

    @Binds
    @Singleton
    abstract fun bindProjectInvitationRepository(impl: ProjectInvitationRepositoryImpl): ProjectInvitationRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindProjectRoleRepository(impl: ProjectRoleRepositoryImpl): ProjectRoleRepository

    @Binds
    @Singleton
    abstract fun bindProjectsWrapperRepository(impl: ProjectsWrapperRepositoryImpl): ProjectsWrapperRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

}