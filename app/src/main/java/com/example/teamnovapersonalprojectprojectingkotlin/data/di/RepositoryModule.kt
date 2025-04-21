package com.example.teamnovapersonalprojectprojectingkotlin.data.di

// 모든 Repository 인터페이스와 구현체 Import
import com.example.teamnovapersonalprojectprojectingkotlin.data.repository.*
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 앱 전역에서 사용할 Repository는 SingletonComponent 사용
abstract class RepositoryModule {
    // --- 기존 Binds ---
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds @Singleton abstract fun bindProjectMemberRepository(impl: ProjectMemberRepositoryImpl): ProjectMemberRepository
    @Binds @Singleton abstract fun bindProjectRoleRepository(impl: ProjectRoleRepositoryImpl): ProjectRoleRepository
    @Binds @Singleton abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    // --- 신규 Repository Binds 추가 ---
    @Binds @Singleton abstract fun bindDmRepository(impl: DmRepositoryImpl): DmRepository
    @Binds @Singleton abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
    @Binds @Singleton abstract fun bindProjectSettingRepository(impl: ProjectSettingRepositoryImpl): ProjectSettingRepository
    @Binds @Singleton abstract fun bindProjectStructureRepository(impl: ProjectStructureRepositoryImpl): ProjectStructureRepository

}