package com.example.data.di

import com.example.domain.usecase.user.* // 생성한 UseCase 임포트
import com.example.domain.usecase.project.* // project 패키지 UseCase 임포트
import com.example.domain.usecase.schedule.* // schedule 패키지 UseCase 임포트
import com.example.domain.usecase.auth.* // auth 패키지 UseCase 임포트
import com.example.domain.usecase.friend.*
import com.example.domain.usecase.project.member.*
import com.example.domain.usecase.project.role.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Singleton

/**
 * UseCase 인터페이스와 구현체를 바인딩하는 Hilt 모듈
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindGetUserUseCase(impl: GetUserUseCaseImpl): GetUserUseCase

    @Binds
    abstract fun bindLogoutUseCase(impl: LogoutUseCaseImpl): LogoutUseCase

    @Binds
    abstract fun bindUpdateUserStatusUseCase(impl: UpdateUserStatusUseCaseImpl): UpdateUserStatusUseCase

    @Binds
    abstract fun bindUpdateUserImageUseCase(impl: UpdateUserImageUseCaseImpl): UpdateUserImageUseCase

    // --- Project UseCases ---
    @Binds
    abstract fun bindGetProjectMemberDetailsUseCase(impl: GetProjectMemberDetailsUseCaseImpl): GetProjectMemberDetailsUseCase

    @Binds
    abstract fun bindGetProjectRolesUseCase(impl: GetProjectRolesUseCaseImpl): GetProjectRolesUseCase

    @Binds
    abstract fun bindGetRoleDetailsUseCase(impl: GetRoleDetailsUseCaseImpl): GetRoleDetailsUseCase


    @Binds
    abstract fun bindUpdateMemberRolesUseCase(impl: UpdateMemberRolesUseCaseImpl): UpdateMemberRolesUseCase

    // --- Project Role UseCases ---
    @Binds
    abstract fun bindDeleteRoleUseCase(impl: DeleteRoleUseCaseImpl): DeleteRoleUseCase


    @Binds
    abstract fun bindDeleteProjectRoleUseCase(impl: DeleteProjectRoleUseCaseImpl): DeleteProjectRoleUseCase

    @Binds
    abstract fun bindCreateRoleUseCase(impl: CreateRoleUseCaseImpl): CreateRoleUseCase

    @Binds
    abstract fun bindUpdateRoleUseCase(impl: UpdateRoleUseCaseImpl): UpdateRoleUseCase


    // --- Project Member List UseCases ---
    @Binds
    abstract fun bindObserveProjectMembersUseCase(impl: ObserveProjectMembersUseCaseImpl): ObserveProjectMembersUseCase

    @Binds
    abstract fun bindFetchProjectMembersUseCase(impl: FetchProjectMembersUseCaseImpl): FetchProjectMembersUseCase

    @Binds
    abstract fun bindDeleteProjectMemberUseCase(impl: DeleteProjectMemberUseCaseImpl): DeleteProjectMemberUseCase

    @Binds
    abstract fun bindAddProjectMemberUseCase(impl: AddProjectMemberUseCaseImpl): AddProjectMemberUseCase

    // --- Project UseCases (Common? AddSchedule) ---
    @Binds
    abstract fun bindGetSchedulableProjectsUseCase(impl: GetSchedulableProjectsUseCaseImpl): GetSchedulableProjectsUseCase

    // --- Project UseCases (Common & Setting) ---
    @Binds
    abstract fun bindGetProjectStructureUseCase(impl: GetProjectStructureUseCaseImpl): GetProjectStructureUseCase

    @Binds
    abstract fun bindDeleteCategoryUseCase(impl: DeleteCategoryUseCaseImpl): DeleteCategoryUseCase

    @Binds
    abstract fun bindDeleteChannelUseCase(impl: DeleteChannelUseCaseImpl): DeleteChannelUseCase

    @Binds
    abstract fun bindRenameProjectUseCase(impl: RenameProjectUseCaseImpl): RenameProjectUseCase

    @Binds
    abstract fun bindDeleteProjectUseCase(impl: DeleteProjectUseCaseImpl): DeleteProjectUseCase

    // --- Schedule UseCases (AddSchedule) ---
    @Binds
    abstract fun bindAddScheduleUseCase(impl: AddScheduleUseCaseImpl): AddScheduleUseCase

    @Binds
    abstract fun bindGetSchedulesForDateUseCase(impl: GetSchedulesForDateUseCaseImpl): GetSchedulesForDateUseCase

    @Binds
    abstract fun bindDeleteScheduleUseCase(impl: DeleteScheduleUseCaseImpl): DeleteScheduleUseCase

    @Binds
    abstract fun bindGetScheduleDetailUseCase(impl: GetScheduleDetailUseCaseImpl): GetScheduleDetailUseCase

    @Binds
    abstract fun bindUpdateScheduleUseCase(impl: UpdateScheduleUseCaseImpl): UpdateScheduleUseCase


    @Binds
    abstract fun bindSearchUserByNameUseCase(impl: SearchUserByNameUseCaseImpl): SearchUserByNameUseCase

    // --- Auth UseCases ---
    @Binds
    abstract fun bindCheckAuthenticationStatusUseCase(impl: CheckAuthenticationStatusUseCaseImpl): CheckAuthenticationStatusUseCase

    // TODO: 다른 UseCase들도 여기에 바인딩 추가

    @Binds
    abstract fun bindSendFriendRequestUseCase(impl: SendFriendRequestUseCaseImpl): SendFriendRequestUseCase

} 