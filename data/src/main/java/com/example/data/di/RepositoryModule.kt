package com.example.data.di

import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.collection.CategoryCollectionRepositoryImpl
import com.example.data.repository.CategoryRepositoryImpl
import com.example.data.repository.DMChannelRepositoryImpl
import com.example.data.repository.DMWrapperRepositoryImpl
import com.example.data.repository.FriendRepositoryImpl
import com.example.data.repository.InviteRepositoryImpl
import com.example.data.repository.MemberRepositoryImpl
import com.example.data.repository.MessageAttachmentRepositoryImpl
import com.example.data.repository.MessageRepositoryImpl
import com.example.data.repository.PermissionRepositoryImpl
import com.example.data.repository.ProjectChannelRepositoryImpl
import com.example.data.repository.ProjectRepositoryImpl
import com.example.data.repository.ProjectsWrapperRepositoryImpl
import com.example.data.repository.ReactionRepositoryImpl
import com.example.data.repository.RoleRepositoryImpl
import com.example.data.repository.ScheduleRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.SearchRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.collection.CategoryCollectionRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.DMChannelRepository
import com.example.domain.repository.DMWrapperRepository
import com.example.domain.repository.FriendRepository
import com.example.domain.repository.InviteRepository
import com.example.domain.repository.MemberRepository
import com.example.domain.repository.MessageAttachmentRepository
import com.example.domain.repository.MessageRepository
import com.example.domain.repository.PermissionRepository
import com.example.domain.repository.ProjectChannelRepository
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectsWrapperRepository
import com.example.domain.repository.ReactionRepository
import com.example.domain.repository.MediaRepository
import com.example.domain.repository.RoleRepository
import com.example.domain.repository.ScheduleRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.FileRepository
import com.example.data.repository.MediaRepositoryImpl
import com.example.data.repository.FileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
