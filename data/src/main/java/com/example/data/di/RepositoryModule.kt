package com.example.data.di

import dagger.Binds
import dagger.Module
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