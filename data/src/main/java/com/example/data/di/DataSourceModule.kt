package com.example.data.di

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
import com.example.data.datasource.remote.channel.ChannelRemoteDataSource
import com.example.data.datasource.remote.channel.ChannelRemoteDataSourceImpl
import com.example.data.datasource.remote.message.MessageRemoteDataSource
import com.example.data.datasource.remote.message.MessageRemoteDataSourceImpl
import com.example.data.datasource.remote.projectstructure.ProjectStructureRemoteDataSource
import com.example.data.datasource.remote.projectstructure.ProjectStructureRemoteDataSourceImpl
import com.example.data.datasource.remote.dm.DmRemoteDataSource
import com.example.data.datasource.remote.dm.DmRemoteDataSourceImpl
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
    @Singleton
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
     * InviteRemoteDataSource 인터페이스 요청 시
     * InviteRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindInviteRemoteDataSource(
        inviteRemoteDataSourceImpl: InviteRemoteDataSourceImpl
    ): InviteRemoteDataSource

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
     * ProjectMemberRemoteDataSource 인터페이스 요청 시
     * ProjectMemberRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectMemberRemoteDataSource(
        projectMemberRemoteDataSourceImpl: ProjectMemberRemoteDataSourceImpl
    ): ProjectMemberRemoteDataSource
    
    // /**
    //  * ProjectMemberLocalDataSource 인터페이스 요청 시
    //  * ProjectMemberLocalDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
    //  */
    // @Binds
    // abstract fun bindProjectMemberLocalDataSource(
    //     projectMemberLocalDataSourceImpl: ProjectMemberLocalDataSourceImpl
    // ): ProjectMemberLocalDataSource

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
     * ChannelRemoteDataSource 인터페이스 요청 시
     * ChannelRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindChannelRemoteDataSource(
        channelRemoteDataSourceImpl: ChannelRemoteDataSourceImpl
    ): ChannelRemoteDataSource

    /**
     * MessageRemoteDataSource 인터페이스 요청 시
     * MessageRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindMessageRemoteDataSource(messageRemoteDataSourceImpl: MessageRemoteDataSourceImpl): MessageRemoteDataSource

    // 다른 데이터 소스 인터페이스/구현체 쌍이 있다면 여기에 추가

    @Binds
    @Singleton
    abstract fun bindProjectStructureDataSource(projectLocalDataSourceImpl: ProjectStructureRemoteDataSourceImpl): ProjectStructureRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDmRemoteDataSource(
        dmRemoteDataSourceImpl: DmRemoteDataSourceImpl
    ): DmRemoteDataSource

}