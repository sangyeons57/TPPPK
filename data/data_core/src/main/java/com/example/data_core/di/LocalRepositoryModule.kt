package com.example.data_core.di

import com.example.data_core.repository.local.*
import com.example.domain.repository.local.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Local Repository Module (SSOT Architecture)
 * Room Database 전용 Repository DI 구성
 * 
 * 🎯 역할:
 * - Local Repository 인터페이스와 구현체 바인딩
 * - @Singleton 스코프 적용 (앱 전체 생명주기)
 * - SSOT 패턴에 따른 클린 아키텍처 지원
 * 
 * 📋 바인딩된 Repository 목록:
 * - LocalCategoryRepository ✅
 * - LocalProjectRepository ✅
 * - LocalProjectChannelRepository ✅
 * - LocalMessageAttachmentRepository ✅
 * - LocalDMChannelRepository ✅
 * - LocalDMWrapperRepository ✅
 * - LocalFriendRepository ✅
 * - LocalScheduleRepository ✅
 * - LocalTaskRepository ✅
 * - LocalReactionRepository ✅
 * - LocalMemberRepository ✅
 * - LocalPermissionRepository ✅
 * - LocalProjectRoleRepository ✅
 * - LocalProjectInvitationRepository ✅
 * - LocalProjectsWrapperRepository ✅
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalRepositoryModule {

    // === Core Domain Repositories ===

    @Binds
    @Singleton
    abstract fun bindLocalCategoryRepository(
        implementation: LocalCategoryRepositoryImpl
    ): LocalCategoryRepository

    @Binds
    @Singleton
    abstract fun bindLocalProjectRepository(
        implementation: LocalProjectRepositoryImpl
    ): LocalProjectRepository

    @Binds
    @Singleton
    abstract fun bindLocalProjectChannelRepository(
        implementation: LocalProjectChannelRepositoryImpl
    ): LocalProjectChannelRepository

    // === Message & Communication Repositories ===

    @Binds
    @Singleton
    abstract fun bindLocalMessageAttachmentRepository(
        implementation: LocalMessageAttachmentRepositoryImpl
    ): LocalMessageAttachmentRepository

    @Binds
    @Singleton
    abstract fun bindLocalDMChannelRepository(
        implementation: LocalDMChannelRepositoryImpl
    ): LocalDMChannelRepository

    @Binds
    @Singleton
    abstract fun bindLocalDMWrapperRepository(
        implementation: LocalDMWrapperRepositoryImpl
    ): LocalDMWrapperRepository

    @Binds
    @Singleton
    abstract fun bindLocalReactionRepository(
        implementation: LocalReactionRepositoryImpl
    ): LocalReactionRepository

    // === Social & Member Repositories ===

    @Binds
    @Singleton
    abstract fun bindLocalFriendRepository(
        implementation: LocalFriendRepositoryImpl
    ): LocalFriendRepository

    @Binds
    @Singleton
    abstract fun bindLocalMemberRepository(
        implementation: LocalMemberRepositoryImpl
    ): LocalMemberRepository

    // === Permission & Role Repositories ===

    @Binds
    @Singleton
    abstract fun bindLocalPermissionRepository(
        implementation: LocalPermissionRepositoryImpl
    ): LocalPermissionRepository

    @Binds
    @Singleton
    abstract fun bindLocalProjectRoleRepository(
        implementation: LocalProjectRoleRepositoryImpl
    ): LocalProjectRoleRepository

    // === Invitation & Wrapper Repositories ===

    @Binds
    @Singleton
    abstract fun bindLocalProjectInvitationRepository(
        implementation: LocalProjectInvitationRepositoryImpl
    ): LocalProjectInvitationRepository

    @Binds
    @Singleton
    abstract fun bindLocalProjectsWrapperRepository(
        implementation: LocalProjectsWrapperRepositoryImpl
    ): LocalProjectsWrapperRepository

    // === Schedule & Task Repositories ===

    @Binds
    @Singleton
    abstract fun bindLocalScheduleRepository(
        implementation: LocalScheduleRepositoryImpl
    ): LocalScheduleRepository

    @Binds
    @Singleton
    abstract fun bindLocalTaskRepository(
        implementation: LocalTaskRepositoryImpl
    ): LocalTaskRepository
}