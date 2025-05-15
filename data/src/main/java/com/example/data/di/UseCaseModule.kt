package com.example.data.di

import com.example.domain.usecase.user.* // 생성한 UseCase 임포트
import com.example.domain.usecase.project.* // project 패키지 UseCase 임포트
import com.example.domain.usecase.schedule.* // schedule 패키지 UseCase 임포트
import com.example.domain.usecase.auth.* // auth 패키지 UseCase 임포트
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * UseCase 인터페이스와 구현체를 바인딩하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindGetUserUseCase(impl: GetUserUseCaseImpl): GetUserUseCase

    @Binds
    @Singleton
    abstract fun bindLogoutUseCase(impl: LogoutUseCaseImpl): LogoutUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateUserStatusUseCase(impl: UpdateUserStatusUseCaseImpl): UpdateUserStatusUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateUserImageUseCase(impl: UpdateUserImageUseCaseImpl): UpdateUserImageUseCase

    // --- Project UseCases ---
    @Binds
    @Singleton
    abstract fun bindGetProjectMemberDetailsUseCase(impl: GetProjectMemberDetailsUseCaseImpl): GetProjectMemberDetailsUseCase

    @Binds
    @Singleton
    abstract fun bindGetProjectRolesUseCase(impl: GetProjectRolesUseCaseImpl): GetProjectRolesUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateMemberRolesUseCase(impl: UpdateMemberRolesUseCaseImpl): UpdateMemberRolesUseCase

    // --- Project Role UseCases ---
    @Binds
    @Singleton
    abstract fun bindGetRoleDetailsUseCase(impl: GetRoleDetailsUseCaseImpl): GetRoleDetailsUseCase

    @Binds
    @Singleton
    abstract fun bindCreateRoleUseCase(impl: CreateRoleUseCaseImpl): CreateRoleUseCase

    @Binds
    @Singleton
    abstract fun bindUpdateRoleUseCase(impl: UpdateRoleUseCaseImpl): UpdateRoleUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteRoleUseCase(impl: DeleteRoleUseCaseImpl): DeleteRoleUseCase

    // --- Project Member List UseCases ---
    @Binds
    @Singleton
    abstract fun bindObserveProjectMembersUseCase(impl: ObserveProjectMembersUseCaseImpl): ObserveProjectMembersUseCase

    @Binds
    @Singleton
    abstract fun bindFetchProjectMembersUseCase(impl: FetchProjectMembersUseCaseImpl): FetchProjectMembersUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteProjectMemberUseCase(impl: DeleteProjectMemberUseCaseImpl): DeleteProjectMemberUseCase

    // --- Project UseCases (Common? AddSchedule) ---
    @Binds
    @Singleton
    abstract fun bindGetSchedulableProjectsUseCase(impl: GetSchedulableProjectsUseCaseImpl): GetSchedulableProjectsUseCase

    // --- Project UseCases (Common & Setting) ---
    @Binds
    @Singleton
    abstract fun bindGetProjectStructureUseCase(impl: GetProjectStructureUseCaseImpl): GetProjectStructureUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteCategoryUseCase(impl: DeleteCategoryUseCaseImpl): DeleteCategoryUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteChannelUseCase(impl: DeleteChannelUseCaseImpl): DeleteChannelUseCase

    @Binds
    @Singleton
    abstract fun bindRenameProjectUseCase(impl: RenameProjectUseCaseImpl): RenameProjectUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteProjectUseCase(impl: DeleteProjectUseCaseImpl): DeleteProjectUseCase

    // --- Schedule UseCases (AddSchedule) ---
    @Binds
    @Singleton
    abstract fun bindAddScheduleUseCase(impl: AddScheduleUseCaseImpl): AddScheduleUseCase

    @Binds
    @Singleton
    abstract fun bindGetSchedulesForDateUseCase(impl: GetSchedulesForDateUseCaseImpl): GetSchedulesForDateUseCase

    @Binds
    @Singleton
    abstract fun bindDeleteScheduleUseCase(impl: DeleteScheduleUseCaseImpl): DeleteScheduleUseCase

    @Binds
    @Singleton
    abstract fun bindGetScheduleDetailUseCase(impl: GetScheduleDetailUseCaseImpl): GetScheduleDetailUseCase

    // --- Auth UseCases ---
    @Binds
    @Singleton
    abstract fun bindCheckAuthenticationStatusUseCase(impl: CheckAuthenticationStatusUseCaseImpl): CheckAuthenticationStatusUseCase

    // TODO: 다른 UseCase들도 여기에 바인딩 추가

} 