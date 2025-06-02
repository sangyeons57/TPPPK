package com.example.data.di

import com.example.domain.usecase.user.* // 생성한 UseCase 임포트
import com.example.domain.usecase.project.* // project 패키지 UseCase 임포트
import com.example.domain.usecase.schedule.* // schedule 패키지 UseCase 임포트
import com.example.domain.usecase.auth.GetAuthErrorMessageUseCase
import com.example.domain.usecase.auth.GetAuthErrorMessageUseCaseImpl
import com.example.domain.usecase.auth.CheckAuthenticationStatusUseCase
import com.example.domain.usecase.auth.CheckAuthenticationStatusUseCaseImpl
import com.example.domain.usecase.friend.*
import com.example.domain.usecase.project.member.*
import com.example.domain.usecase.project.role.*
import com.example.domain.usecase.project.ConvertProjectStructureToDraggableItemsUseCase
import com.example.domain.usecase.project.ConvertProjectStructureToDraggableItemsUseCaseImpl
import com.example.domain.usecase.project.MoveChannelUseCase
import com.example.domain.usecase.project.MoveChannelUseCaseImpl
import com.example.domain.usecase.project.MoveCategoryUseCase
import com.example.domain.usecase.project.MoveCategoryUseCaseImpl
import com.example.domain.usecase.project.AddCategoryUseCase
import com.example.domain.usecase.project.AddCategoryUseCaseImpl
import com.example.domain.usecase.project.DeleteCategoryUseCase
import com.example.domain.usecase.project.DeleteCategoryUseCaseImpl
import com.example.domain.usecase.project.DeleteChannelUseCase
import com.example.domain.usecase.project.DeleteChannelUseCaseImpl
import com.example.domain.usecase.project.RenameCategoryUseCase
import com.example.domain.usecase.project.RenameCategoryUseCaseImpl
import com.example.domain.usecase.project.RenameChannelUseCase
import com.example.domain.usecase.project.RenameChannelUseCaseImpl
import com.example.domain.usecase.project.AddChannelUseCase
import com.example.domain.usecase.project.AddChannelUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * UseCase 인터페이스와 구현체를 바인딩하는 Hilt 모듈
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindGetUserUseCase(impl: GetUserUseCaseImpl): GetUserUseCase

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


    // --- Project Structure UseCases ---
    @Binds
    abstract fun bindConvertProjectStructureToDraggableItemsUseCase(
        impl: ConvertProjectStructureToDraggableItemsUseCaseImpl
    ): ConvertProjectStructureToDraggableItemsUseCase
    
    @Binds
    abstract fun bindMoveChannelUseCase(
        impl: MoveChannelUseCaseImpl
    ): MoveChannelUseCase
    
    @Binds
    abstract fun bindMoveCategoryUseCase(
        impl: MoveCategoryUseCaseImpl
    ): MoveCategoryUseCase
    
    @Binds
    abstract fun bindAddCategoryUseCase(
        impl: AddCategoryUseCaseImpl
    ): AddCategoryUseCase
    

    @Binds
    abstract fun bindRenameCategoryUseCase(
        impl: RenameCategoryUseCaseImpl
    ): RenameCategoryUseCase
    
    @Binds
    abstract fun bindRenameChannelUseCase(
        impl: RenameChannelUseCaseImpl
    ): RenameChannelUseCase
    
    @Binds
    abstract fun bindAddChannelUseCase(
        impl: AddChannelUseCaseImpl
    ): AddChannelUseCase

    // --- Project Member List UseCases ---
    @Binds
    abstract fun bindObserveProjectMembersUseCase(impl: ObserveProjectMembersUseCaseImpl): ObserveProjectMembersUseCase


    @Binds
    abstract fun bindDeleteProjectMemberUseCase(impl: DeleteProjectMemberUseCaseImpl): DeleteProjectMemberUseCase

    @Binds
    abstract fun bindAddProjectMemberUseCase(impl: AddProjectMemberUseCaseImpl): AddProjectMemberUseCase

    // --- Project UseCases (Common? AddSchedule) ---
    @Binds
    abstract fun bindGetUserParticipatingProjectsUseCase(impl: GetUserParticipatingProjectsUseCaseImpl): GetUserParticipatingProjectsUseCase

    // --- Project UseCases (Common & Setting) ---
    @Binds
    abstract fun bindGetProjectStructureUseCase(impl: GetProjectAllCategoriesUseCaseImpl): GetProjectAllCategoriesUseCase

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

    @Binds
    abstract fun bindGetAuthErrorMessageUseCase(impl: GetAuthErrorMessageUseCaseImpl): GetAuthErrorMessageUseCase

    // TODO: 다른 UseCase들도 여기에 바인딩 추가

    @Binds
    abstract fun bindSendFriendRequestUseCase(impl: SendFriendRequestUseCaseImpl): SendFriendRequestUseCase

} 