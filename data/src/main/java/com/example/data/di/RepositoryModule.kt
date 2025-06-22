package com.example.data.di

import com.example.data.repository.base.AuthRepositoryImpl
import com.example.data.repository.base.CategoryRepositoryImpl
import com.example.data.repository.base.DMChannelRepositoryImpl
import com.example.data.repository.base.DMWrapperRepositoryImpl
import com.example.data.repository.base.FriendRepositoryImpl
import com.example.data.repository.base.InviteRepositoryImpl
import com.example.data.repository.base.MemberRepositoryImpl
import com.example.data.repository.base.MessageAttachmentRepositoryImpl
import com.example.data.repository.base.MessageRepositoryImpl
import com.example.data.repository.base.PermissionRepositoryImpl
import com.example.data.repository.base.ProjectChannelRepositoryImpl
import com.example.data.repository.base.ProjectRepositoryImpl
import com.example.data.repository.base.ProjectsWrapperRepositoryImpl
import com.example.data.repository.base.RoleRepositoryImpl
import com.example.data.repository.base.ScheduleRepositoryImpl
import com.example.data.repository.base.UserRepositoryImpl
import com.example.data.repository.base.SearchRepositoryImpl
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.collection.CategoryCollectionRepository
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.base.DMWrapperRepository
import com.example.domain.repository.base.FriendRepository
import com.example.domain.repository.base.InviteRepository
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.MessageAttachmentRepository
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.base.PermissionRepository
import com.example.domain.repository.base.ProjectChannelRepository
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.base.RoleRepository
import com.example.domain.repository.base.ScheduleRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.base.SearchRepository
import com.example.domain.repository.base.FileRepository
import com.example.data.repository.base.MediaRepositoryImpl
import com.example.data.repository.base.FileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
@[Module]
@[InstallIn(SingletonComponent::class)]
abstract class RepositoryModule {

    @[Binds]
    @[Singleton]
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @[Binds]
    @[Singleton]
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @[Binds]
    @[Singleton]
    abstract fun bindDMChannelRepository(impl: DMChannelRepositoryImpl): DMChannelRepository

    @[Binds]
    @[Singleton]
    abstract fun bindDMWrapperRepository(impl: DMWrapperRepositoryImpl): DMWrapperRepository

    @[Binds]
    @[Singleton]
    abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository

    @[Binds]
    @[Singleton]
    abstract fun bindInviteRepository(impl: InviteRepositoryImpl): InviteRepository

    @[Binds]
    @[Singleton]
    abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository

    @[Binds]
    @[Singleton]
    abstract fun bindMessageAttachmentRepository(impl: MessageAttachmentRepositoryImpl): MessageAttachmentRepository

    @[Binds]
    @[Singleton]
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @[Binds]
    @[Singleton]
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository

    @[Binds]
    @[Singleton]
    abstract fun bindProjectChannelRepository(impl: ProjectChannelRepositoryImpl): ProjectChannelRepository

    @[Binds]
    @[Singleton]
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @[Binds]
    @[Singleton]
    abstract fun bindProjectsWrapperRepository(impl: ProjectsWrapperRepositoryImpl): ProjectsWrapperRepository

    @[Binds]
    @[Singleton]
    abstract fun bindReactionRepository(impl: ReactionRepositoryImpl): ReactionRepository
    
    @[Binds]
    @[Singleton]
    abstract fun bindRoleRepository(impl: RoleRepositoryImpl): RoleRepository

    @[Binds]
    @[Singleton]
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @[Binds]
    @[Singleton]
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    
    @[Binds]
    @[Singleton]
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @[Binds]
    @[Singleton]
    abstract fun bindCategoryCollectionRepository(impl: CategoryCollectionRepositoryImpl): CategoryCollectionRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
}
*/