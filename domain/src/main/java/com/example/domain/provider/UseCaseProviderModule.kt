package com.example.domain.provider

import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.auth.AuthRegistrationUseCaseProvider
import com.example.domain.provider.auth.AuthPasswordUseCaseProvider
import com.example.domain.provider.auth.AuthAccountUseCaseProvider
import com.example.domain.provider.auth.AuthValidationUseCaseProvider
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.friend.FriendUseCaseProvider
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.ProjectAssetsUseCaseProvider
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.domain.provider.project.ProjectMemberUseCaseProvider
import com.example.domain.provider.project.ProjectRoleUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.domain.provider.schedule.ScheduleUseCaseProvider
import com.example.domain.provider.search.SearchUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.domain.provider.validation.ValidationUseCaseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * UseCase Provider들을 Hilt DI에 등록하는 모듈
 * 
 * 모든 UseCaseProvider들을 싱글톤으로 제공하여 앱 전체에서 접근 가능하게 합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseProviderModule {

    @Provides
    @Singleton
    fun provideAuthSessionUseCaseProvider(provider: AuthSessionUseCaseProvider): AuthSessionUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideAuthRegistrationUseCaseProvider(provider: AuthRegistrationUseCaseProvider): AuthRegistrationUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideAuthPasswordUseCaseProvider(provider: AuthPasswordUseCaseProvider): AuthPasswordUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideAuthAccountUseCaseProvider(provider: AuthAccountUseCaseProvider): AuthAccountUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideAuthValidationUseCaseProvider(provider: AuthValidationUseCaseProvider): AuthValidationUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideValidationUseCaseProvider(provider: ValidationUseCaseProvider): ValidationUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideUserUseCaseProvider(provider: UserUseCaseProvider): UserUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideCoreProjectUseCaseProvider(provider: CoreProjectUseCaseProvider): CoreProjectUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideProjectStructureUseCaseProvider(provider: ProjectStructureUseCaseProvider): ProjectStructureUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideProjectChannelUseCaseProvider(provider: ProjectChannelUseCaseProvider): ProjectChannelUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideProjectMemberUseCaseProvider(provider: ProjectMemberUseCaseProvider): ProjectMemberUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideProjectRoleUseCaseProvider(provider: ProjectRoleUseCaseProvider): ProjectRoleUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideProjectAssetsUseCaseProvider(provider: ProjectAssetsUseCaseProvider): ProjectAssetsUseCaseProvider = provider


    @Provides
    @Singleton
    fun provideDMUseCaseProvider(provider: DMUseCaseProvider): DMUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideFriendUseCaseProvider(provider: FriendUseCaseProvider): FriendUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideScheduleUseCaseProvider(provider: ScheduleUseCaseProvider): ScheduleUseCaseProvider = provider

    @Provides
    @Singleton
    fun provideSearchUseCaseProvider(provider: SearchUseCaseProvider): SearchUseCaseProvider = provider
}