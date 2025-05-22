package com.example.data.di

// 모든 Repository 인터페이스와 구현체 Import
import com.example.data.datasource.remote.auth.AuthRemoteDataSource
import com.example.data.datasource.remote.auth.AuthRemoteDataSourceImpl
import com.example.data.datasource.remote.user.UserRemoteDataSource
import com.example.data.datasource.remote.user.UserRemoteDataSourceImpl
import com.example.data.model.mapper.UserMapper
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.ChannelRepositoryImpl
import com.example.data.repository.FriendRepositoryImpl
import com.example.data.repository.InviteRepositoryImpl
import com.example.data.repository.ProjectChannelRepositoryImpl
import com.example.data.repository.ProjectMemberRepositoryImpl
import com.example.data.repository.ProjectRepositoryImpl
import com.example.data.repository.ProjectRoleRepositoryImpl
import com.example.data.repository.ProjectSettingRepositoryImpl
import com.example.data.repository.ProjectStructureRepositoryImpl
import com.example.data.repository.ScheduleRepositoryImpl
import com.example.data.repository.SearchRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.MessageRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ChannelRepository
import com.example.domain.repository.FriendRepository
import com.example.domain.repository.InviteRepository
import com.example.domain.repository.ProjectChannelRepository
import com.example.domain.repository.ProjectMemberRepository
import com.example.domain.repository.ProjectRepository
import com.example.domain.repository.ProjectRoleRepository
import com.example.domain.repository.ProjectSettingRepository
import com.example.domain.repository.ProjectStructureRepository
import com.example.domain.repository.ScheduleRepository
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.MessageRepository
import com.example.domain.repository.DmRepository
import com.example.data.repository.DmRepositoryImpl
import com.example.domain.util.NetworkConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 레포지토리 및 데이터 소스 구현체를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class) // 앱 전역에서 사용할 Repository는 SingletonComponent 사용
abstract class RepositoryModule {
    // --- 기존 Binds ----
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds @Singleton abstract fun bindProjectMemberRepository(impl: ProjectMemberRepositoryImpl): ProjectMemberRepository
    @Binds @Singleton abstract fun bindProjectRoleRepository(impl: ProjectRoleRepositoryImpl): ProjectRoleRepository
    @Binds @Singleton abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    // --- 신규 Repository Binds 추가 ---
    /**
     * @deprecated DM 기능은 ChannelRepository로 통합되었습니다.
     */

    @Binds @Singleton abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
    @Binds @Singleton abstract fun bindProjectSettingRepository(impl: ProjectSettingRepositoryImpl): ProjectSettingRepository
    @Binds @Singleton abstract fun bindProjectStructureRepository(impl: ProjectStructureRepositoryImpl): ProjectStructureRepository
    @Binds @Singleton abstract fun bindInviteRepository(impl: InviteRepositoryImpl): InviteRepository
    @Binds @Singleton abstract fun bindProjectChannelRepository(impl: ProjectChannelRepositoryImpl): ProjectChannelRepository
    @Binds @Singleton abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    /**
     * AuthRemoteDataSource 구현체를 주입합니다.
     *
     * @param dataSourceImpl AuthRemoteDataSourceImpl 구현체
     * @return AuthRemoteDataSource 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        dataSourceImpl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    /**
     * UserRemoteDataSource 구현체를 주입합니다.
     *
     * @param dataSourceImpl UserRemoteDataSourceImpl 구현체
     * @return UserRemoteDataSource 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        dataSourceImpl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindMessageRepository(messageRepositoryImpl: MessageRepositoryImpl): MessageRepository

    // 추가: DmRepository 바인딩
    @Binds
    @Singleton
    abstract fun bindDmRepository(impl: DmRepositoryImpl): DmRepository
}