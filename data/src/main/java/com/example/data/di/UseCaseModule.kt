package com.example.data.di

/**
 * UseCase 인터페이스와 구현체를 바인딩하는 Hilt 모듈
 */
/**
@Module
@InstallIn(ViewModelComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindGetUserUseCase(impl: GetUserUseCaseImpl): GetUserUseCase

    @Binds
    abstract fun bindUpdateUserStatusUseCase(impl: UpdateUserStatusUseCaseImpl): UpdateUserStatusUseCase

    @Binds
    abstract fun bindUpdateUserImageUseCase(impl: UpdateUserImageUseCaseImpl): UpdateUserImageUseCase

    @Binds
    abstract fun bindUpdateUserMemoUseCase(impl: UpdateUserMemoUseCaseImpl): UpdateUserMemoUseCase

    @Binds
    abstract fun bindGetCurrentStatusUseCase(impl: GetCurrentStatusUseCaseImpl): GetCurrentStatusUseCase

    @Binds
    abstract fun bindGetCurrentUserStreamUseCase(impl: GetCurrentUserStreamUseCaseImpl): GetCurrentUserStreamUseCase

    @Binds
    abstract fun bindGetUserInfoUseCase(impl: GetUserInfoUseCaseImpl): GetUserInfoUseCase

    @Binds
    abstract fun bindCheckNicknameAvailabilityUseCase(impl: CheckNicknameAvailabilityUseCaseImpl): CheckNicknameAvailabilityUseCase

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
        impl: AddProjectChannelUseCaseImpl
    ): AddProjectChannelUseCase

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

    @Binds
    abstract fun bindSearchUsersByNameUseCase(impl: SearchUsersByNameUseCaseImpl): SearchUsersByNameUseCase

    // --- Auth UseCases ---
    @Binds
    abstract fun bindCheckAuthenticationStatusUseCase(impl: CheckAuthenticationStatusUseCaseImpl): CheckAuthenticationStatusUseCase

    @Binds
    abstract fun bindGetAuthErrorMessageUseCase(impl: GetAuthErrorMessageUseCaseImpl): GetAuthErrorMessageUseCase

    @Binds
    abstract fun bindWithdrawMembershipUseCase(impl: WithdrawMembershipUseCaseImpl): WithdrawMembershipUseCase

    // TODO: 다른 UseCase들도 여기에 바인딩 추가

    @Binds
    abstract fun bindSendFriendRequestUseCase(impl: SendFriendRequestUseCaseImpl): SendFriendRequestUseCase

    @Binds
    abstract fun bindGetCategoryDetailUseCase(impl: GetCategoryDetailsUseCaseImpl): GetCategoryDetailsUseCase

    @Binds
    abstract fun bindUpdateCategoryUseCase(impl: UpdateCategoryUseCaseImpl): UpdateCategoryUseCase

    @Binds
    abstract fun bindGetProjectChannelDetailUseCase(impl: GetProjectChannelDetailsUseCaseImpl): GetProjectChannelDetailsUseCase

    @Binds
    abstract fun bindUpdateProjectChannelUseCase(impl: UpdateProjectChannelUseCaseImpl): UpdateProjectChannelUseCase
}
 */