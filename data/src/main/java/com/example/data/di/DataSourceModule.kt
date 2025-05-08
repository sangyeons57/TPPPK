package com.example.data.di

import com.example.data.datasource.local.dm.DmLocalDataSource
import com.example.data.datasource.local.dm.DmLocalDataSourceImpl
import com.example.data.datasource.local.friend.FriendLocalDataSource
import com.example.data.datasource.local.friend.FriendLocalDataSourceImpl
import com.example.data.datasource.local.invite.InviteLocalDataSource
import com.example.data.datasource.local.invite.InviteLocalDataSourceImpl
import com.example.data.datasource.local.project.ProjectLocalDataSource
import com.example.data.datasource.local.project.ProjectLocalDataSourceImpl
import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource
import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSourceImpl
import com.example.data.datasource.local.projectrole.ProjectRoleLocalDataSource
import com.example.data.datasource.local.projectrole.ProjectRoleLocalDataSourceImpl
import com.example.data.datasource.remote.chat.ChatRemoteDataSource
import com.example.data.datasource.remote.chat.ChatRemoteDataSourceImpl
import com.example.data.datasource.remote.dm.DmRemoteDataSource
import com.example.data.datasource.remote.dm.DmRemoteDataSourceImpl
import com.example.data.datasource.remote.friend.FriendRemoteDataSource
import com.example.data.datasource.remote.friend.FriendRemoteDataSourceImpl
import com.example.data.datasource.remote.invite.InviteRemoteDataSource
import com.example.data.datasource.remote.invite.InviteRemoteDataSourceImpl
import com.example.data.datasource.remote.projectmember.ProjectMemberRemoteDataSource
import com.example.data.datasource.remote.projectmember.ProjectMemberRemoteDataSourceImpl
import com.example.data.datasource.remote.projectrole.ProjectRoleRemoteDataSource
import com.example.data.datasource.remote.projectrole.ProjectRoleRemoteDataSourceImpl
import com.example.data.datasource.remote.schedule.ScheduleRemoteDataSource
import com.example.data.datasource.remote.schedule.ScheduleRemoteDataSourceImpl
import com.example.data.datasource.remote.project.ProjectRemoteDataSource
import com.example.data.datasource.remote.project.ProjectRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 데이터 소스 관련 의존성을 Hilt에 바인딩하는 모듈입니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    /**
     * ScheduleRemoteDataSource 인터페이스 요청 시
     * ScheduleRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton // Impl이 Singleton이므로 Binds도 Singleton 스코프 지정 가능
    abstract fun bindScheduleRemoteDataSource(
        scheduleRemoteDataSourceImpl: ScheduleRemoteDataSourceImpl
    ): ScheduleRemoteDataSource

    /**
     * ProjectRoleRemoteDataSource 인터페이스 요청 시
     * ProjectRoleRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectRoleRemoteDataSource(
        projectRoleRemoteDataSourceImpl: ProjectRoleRemoteDataSourceImpl
    ): ProjectRoleRemoteDataSource

    /**
     * ProjectRoleLocalDataSource 인터페이스 요청 시
     * ProjectRoleLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectRoleLocalDataSource(
        projectRoleLocalDataSourceImpl: ProjectRoleLocalDataSourceImpl
    ): ProjectRoleLocalDataSource
    
    /**
     * ChatRemoteDataSource 인터페이스 요청 시
     * ChatRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindChatRemoteDataSource(
        chatRemoteDataSourceImpl: ChatRemoteDataSourceImpl
    ): ChatRemoteDataSource

    /**
     * DmRemoteDataSource 인터페이스 요청 시
     * DmRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindDmRemoteDataSource(
        dmRemoteDataSourceImpl: DmRemoteDataSourceImpl
    ): DmRemoteDataSource

    /**
     * DmLocalDataSource 인터페이스 요청 시
     * DmLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindDmLocalDataSource(
        dmLocalDataSourceImpl: DmLocalDataSourceImpl
    ): DmLocalDataSource
    
    /**
     * InviteRemoteDataSource 인터페이스 요청 시
     * InviteRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindInviteRemoteDataSource(
        inviteRemoteDataSourceImpl: InviteRemoteDataSourceImpl
    ): InviteRemoteDataSource
    
    /**
     * InviteLocalDataSource 인터페이스 요청 시
     * InviteLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindInviteLocalDataSource(
        inviteLocalDataSourceImpl: InviteLocalDataSourceImpl
    ): InviteLocalDataSource

    /**
     * FriendRemoteDataSource 인터페이스 요청 시
     * FriendRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindFriendRemoteDataSource(
        friendRemoteDataSourceImpl: FriendRemoteDataSourceImpl
    ): FriendRemoteDataSource
    
    /**
     * FriendLocalDataSource 인터페이스 요청 시
     * FriendLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindFriendLocalDataSource(
        friendLocalDataSourceImpl: FriendLocalDataSourceImpl
    ): FriendLocalDataSource
    
    /**
     * ProjectMemberRemoteDataSource 인터페이스 요청 시
     * ProjectMemberRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectMemberRemoteDataSource(
        projectMemberRemoteDataSourceImpl: ProjectMemberRemoteDataSourceImpl
    ): ProjectMemberRemoteDataSource
    
    /**
     * ProjectMemberLocalDataSource 인터페이스 요청 시
     * ProjectMemberLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectMemberLocalDataSource(
        projectMemberLocalDataSourceImpl: ProjectMemberLocalDataSourceImpl
    ): ProjectMemberLocalDataSource

    /**
     * ProjectRemoteDataSource 인터페이스 요청 시
     * ProjectRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectRemoteDataSource(
        projectRemoteDataSourceImpl: ProjectRemoteDataSourceImpl
    ): ProjectRemoteDataSource

    /**
     * ProjectLocalDataSource 인터페이스 요청 시
     * ProjectLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectLocalDataSource(
        projectLocalDataSourceImpl: ProjectLocalDataSourceImpl
    ): ProjectLocalDataSource

    // 다른 데이터 소스 인터페이스/구현체 쌍이 있다면 여기에 추가
} 