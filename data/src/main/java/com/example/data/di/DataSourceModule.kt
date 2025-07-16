package com.example.data.di

import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.DMChannelRemoteDataSourceImpl
import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.data.datasource.remote.DMWrapperRemoteDataSourceImpl
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.FriendRemoteDataSourceImpl
import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.data.datasource.remote.MemberRemoteDataSourceImpl
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSource
import com.example.data.datasource.remote.MessageAttachmentRemoteDataSourceImpl
import com.example.data.datasource.remote.MessageRemoteDataSource
import com.example.data.datasource.remote.MessageRemoteDataSourceImpl
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.datasource.remote.PermissionRemoteDataSourceImpl
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource
import com.example.data.datasource.remote.ProjectChannelRemoteDataSourceImpl
import com.example.data.datasource.remote.ProjectRemoteDataSource
import com.example.data.datasource.remote.ProjectRemoteDataSourceImpl
import com.example.data.datasource.remote.ProjectInvitationRemoteDataSource
import com.example.data.datasource.remote.ProjectInvitationRemoteDataSourceImpl
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.datasource.remote.RoleRemoteDataSourceImpl
import com.example.data.datasource.remote.ScheduleRemoteDataSource
import com.example.data.datasource.remote.ScheduleRemoteDataSourceImpl
import com.example.data.datasource.remote.TaskRemoteDataSource
import com.example.data.datasource.remote.TaskRemoteDataSourceImpl
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSourceImpl
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.CategoryRemoteDataSourceImpl
import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSourceImpl
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.datasource.remote.special.AuthRemoteDataSourceImpl
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.datasource.remote.special.FileDataSource
import com.example.data.datasource.remote.special.FileDataSourceImpl
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSourceImpl
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
    abstract fun bindRoleRemoteDataSource(
        roleRemoteDataSourceImpl: RoleRemoteDataSourceImpl
    ): RoleRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPermissionRemoteDataSource(
        roleRemoteDataSourceImpl: PermissionRemoteDataSourceImpl
    ): PermissionRemoteDataSource



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
        projectMemberRemoteDataSourceImpl: MemberRemoteDataSourceImpl
    ): MemberRemoteDataSource
    
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
     * ProjectInvitationRemoteDataSource 인터페이스 요청 시
     * ProjectInvitationRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectInvitationRemoteDataSource(
        projectInvitationRemoteDataSourceImpl: ProjectInvitationRemoteDataSourceImpl
    ): ProjectInvitationRemoteDataSource

    /**
     * ChannelRemoteDataSource 인터페이스 요청 시
     * ChannelRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindProjectChannelRemoteDataSource(
        channelRemoteDataSourceImpl: ProjectChannelRemoteDataSourceImpl
    ): ProjectChannelRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDMChannelRemoteDataSource(
        channelRemoteDataSourceImpl: DMChannelRemoteDataSourceImpl
    ): DMChannelRemoteDataSource


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
    abstract fun bindDMWrapperRemoteDataSource(
        dmWrapperRemoteDataSourceImpl: DMWrapperRemoteDataSourceImpl
    ): DMWrapperRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindMessageAttachmentRemoteDataSource(
        messageAttachmentRemoteDataSource: MessageAttachmentRemoteDataSourceImpl
    ): MessageAttachmentRemoteDataSource


    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        userRemoteDataSourceImpl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource
    
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        authRemoteDataSourceImpl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCategoryRemoteDataSource(
        categoryRemoteDataSourceImpl: CategoryRemoteDataSourceImpl
    ): CategoryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindProjectsWrapperRemoteDataSource(
        projectsWrapperRemoteDataSourceImpl: ProjectsWrapperRemoteDataSourceImpl
    ): ProjectsWrapperRemoteDataSource

    /**
     * FileDataSource 인터페이스 요청 시
     * FileDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindFileDataSource(
        fileDataSourceImpl: FileDataSourceImpl
    ): FileDataSource

    /**
     * FunctionsRemoteDataSource 인터페이스 요청 시
     * FunctionsRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindFunctionsRemoteDataSource(
        functionsRemoteDataSourceImpl: FunctionsRemoteDataSourceImpl
    ): FunctionsRemoteDataSource

    /**
     * TaskRemoteDataSource 인터페이스 요청 시
     * TaskRemoteDataSourceImpl 구현체를 제공하도록 Hilt에 알립니다.
     */
    @Binds
    @Singleton
    abstract fun bindTaskRemoteDataSource(
        taskRemoteDataSourceImpl: TaskRemoteDataSourceImpl
    ): TaskRemoteDataSource

    /**
     * 지금 다른 방식 사용중
    @Binds
    @Singleton
    abstract fun bindDefaultDataSource(
        defaultDatasourceImpl: DefaultDatasourceImpl
    ): DefaultDatasource
    **/
}