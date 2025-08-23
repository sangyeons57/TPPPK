package com.example.domain_usecase.provider.project

import com.example.domain.usecase.project.DeleteProjectMemberUseCase
import com.example.domain.usecase.project.DeleteProjectMemberUseCaseImpl
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCase
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCaseImpl
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.DMChannelRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.base.ProjectInvitationRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.domain_repository.base.UserRepository
import com.example.domain_usecase.usecase.dm.AddDmChannelUseCase
import com.example.domain_usecase.usecase.dm.GetDmChannelUseCase
import com.example.domain_usecase.usecase.project.core.JoinProjectByIdUseCase
import com.example.domain_usecase.usecase.project.invitation.AcceptProjectInvitationUseCase
import com.example.domain_usecase.usecase.project.invitation.AcceptProjectInvitationUseCaseImpl
import com.example.domain_usecase.usecase.project.invitation.SendProjectInvitationUseCase
import com.example.domain_usecase.usecase.project.invitation.SendProjectInvitationUseCaseImpl
import com.example.domain_usecase.usecase.project.member.AcceptProjectInviteFromMessageUseCase
import com.example.domain_usecase.usecase.project.member.AcceptProjectInviteFromMessageUseCaseImpl
import com.example.domain_usecase.usecase.project.member.AddProjectMemberUseCase
import com.example.domain_usecase.usecase.project.member.AddProjectMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.BlockMemberUseCase
import com.example.domain_usecase.usecase.project.member.BlockMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.CanManageTargetMemberUseCase
import com.example.domain_usecase.usecase.project.member.CanManageTargetMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.CheckUserProjectMembershipUseCase
import com.example.domain_usecase.usecase.project.member.CheckUserProjectMembershipUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetBlockedProjectMembersUseCase
import com.example.domain_usecase.usecase.project.member.GetBlockedProjectMembersUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetProjectMemberUseCase
import com.example.domain_usecase.usecase.project.member.GetProjectMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetProjectMembersUseCase
import com.example.domain_usecase.usecase.project.member.GetProjectMembersUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetRoleMemberCountUseCase
import com.example.domain_usecase.usecase.project.member.GetRoleMemberCountUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetUserPermissionsForProjectUseCase
import com.example.domain_usecase.usecase.project.member.GetUserPermissionsForProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetUserRolesForProjectUseCase
import com.example.domain_usecase.usecase.project.member.GetUserRolesForProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.member.HasProjectPermissionUseCase
import com.example.domain_usecase.usecase.project.member.HasProjectPermissionUseCaseImpl
import com.example.domain_usecase.usecase.project.member.IsCurrentUserOwnerUseCase
import com.example.domain_usecase.usecase.project.member.IsCurrentUserOwnerUseCaseImpl
import com.example.domain_usecase.usecase.project.member.IsTargetMemberOwnerUseCase
import com.example.domain_usecase.usecase.project.member.IsTargetMemberOwnerUseCaseImpl
import com.example.domain_usecase.usecase.project.member.LeaveProjectUseCase
import com.example.domain_usecase.usecase.project.member.LeaveProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.member.ObserveBlockedProjectMembersUseCase
import com.example.domain_usecase.usecase.project.member.ObserveBlockedProjectMembersUseCaseImpl
import com.example.domain_usecase.usecase.project.member.ObserveProjectMembersUseCase
import com.example.domain_usecase.usecase.project.member.ObserveProjectMembersUseCaseImpl
import com.example.domain_usecase.usecase.project.member.OwnerOrPermissionUseCase
import com.example.domain_usecase.usecase.project.member.OwnerOrPermissionUseCaseImpl
import com.example.domain_usecase.usecase.project.member.PermissionDeniedMessageUseCase
import com.example.domain_usecase.usecase.project.member.PermissionDeniedMessageUseCaseImpl
import com.example.domain_usecase.usecase.project.member.RemoveMemberUseCase
import com.example.domain_usecase.usecase.project.member.RemoveMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.RemoveProjectMemberUseCase
import com.example.domain_usecase.usecase.project.member.RemoveProjectMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.SendProjectInviteMessageUseCase
import com.example.domain_usecase.usecase.project.member.SendProjectInviteMessageUseCaseImpl
import com.example.domain_usecase.usecase.project.member.TransferOwnershipUseCase
import com.example.domain_usecase.usecase.project.member.TransferOwnershipUseCaseImpl
import com.example.domain_usecase.usecase.project.member.UnblockMemberUseCase
import com.example.domain_usecase.usecase.project.member.UnblockMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.UpdateMemberRolesUseCase
import com.example.domain_usecase.usecase.project.member.UpdateMemberRolesUseCaseImpl
import com.example.domain_usecase.usecase.project.member.VerifyProjectMembershipUseCase
import com.example.domain_usecase.usecase.project.member.VerifyProjectMembershipUseCaseImpl
import com.example.websocket.usecase.WebSocketUseCaseProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 멤버 관리 UseCase들을 제공하는 Provider
 * 
 * 멤버 추가, 제거, 조회, 역할 관리 등의 멤버 관련 기능을 담당합니다.
 */
@Singleton
class ProjectMemberUseCaseProvider @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val projectRoleRepository: ProjectRoleRepository,
    private val projectInvitationRepository: ProjectInvitationRepository,
    private val messageRepository: MessageRepository,
    private val dmChannelRepository: DMChannelRepository,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
) {

    private fun createAddDmChannelUseCase(): AddDmChannelUseCase {
        return AddDmChannelUseCase(
            dmChannelRepository = this.dmChannelRepository,
            authRepository = this.authRepository
        )
    }

    private fun createGetDmChannelUseCase(): GetDmChannelUseCase {
        return GetDmChannelUseCase(
            dmRepository = this.dmChannelRepository
        )
    }

    /**
     * 특정 프로젝트의 멤버 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectMemberUseCases {
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        // Project repository should always point to the collection root
        projectRepository.setCollection(CollectionPath.projects)
        // Ensure invitations repo is scoped to root collection for invite flows
        projectInvitationRepository.setCollection(CollectionPath.projectInvitations())

        return ProjectMemberUseCases(
            // 멤버 기본 CRUD
            addProjectMemberUseCase = AddProjectMemberUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            
            getProjectMemberUseCase = GetProjectMemberUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            getProjectMembersUseCase = GetProjectMembersUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),

            getRoleMemberCountUseCase = GetRoleMemberCountUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            
            removeProjectMemberUseCase = RemoveProjectMemberUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            
            // 멤버 고급 관리
            getProjectMemberDetailsUseCase = GetProjectMemberDetailsUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            
            deleteProjectMemberUseCase = DeleteProjectMemberUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),

            removeMemberUseCase = RemoveMemberUseCaseImpl(
                projectRepository = this.projectRepository
            ),
            
            observeProjectMembersUseCase = ObserveProjectMembersUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            
            // 멤버 역할 관리
            updateMemberRolesUseCase = UpdateMemberRolesUseCaseImpl(
                memberRepository = this.memberRepository
            ),
            
            // 프로젝트 나가기 및 소유권 전달
            leaveProjectUseCase = LeaveProjectUseCaseImpl(
                projectRepository = this.projectRepository
            ),
            
            transferOwnershipUseCase = TransferOwnershipUseCaseImpl(
                projectRepository = this.projectRepository,
                memberRepository = this.memberRepository,
                authRepository = this.authRepository,
            ),

            // 프로젝트 참여 (Functions 호출)
            joinProjectByIdUseCase = JoinProjectByIdUseCase(
                projectRepository = this.projectRepository
            ),

            // 프로젝트 초대 메시지 전송
            sendProjectInviteMessageUseCase = SendProjectInviteMessageUseCaseImpl(
                messageRepository = this.messageRepository,
                projectRepository = this.projectRepository,
                authRepository = this.authRepository,
                userRepository = this.userRepository,
                webSocketUseCaseProvider = this.webSocketUseCaseProvider,
            ),

            // 프로젝트 초대 관리
            sendProjectInvitationUseCase = SendProjectInvitationUseCaseImpl(
                projectInvitationRepository = this.projectInvitationRepository
            ),

            acceptProjectInvitationUseCase = AcceptProjectInvitationUseCaseImpl(
                projectInvitationRepository = this.projectInvitationRepository
            ),

            acceptProjectInviteFromMessageUseCase = AcceptProjectInviteFromMessageUseCaseImpl(
                projectInvitationRepository = projectInvitationRepository,
                projectRepository = this.projectRepository,
                acceptProjectInvitationUseCase = AcceptProjectInvitationUseCaseImpl(
                    projectInvitationRepository = this.projectInvitationRepository
                )
            ),

            // ViewModel 오케스트레이션용 DM 유틸 UseCase들
            addDmChannelUseCase = createAddDmChannelUseCase(),
            getDmChannelUseCase = createGetDmChannelUseCase(),

            // 멤버십 확인
            checkUserProjectMembershipUseCase = CheckUserProjectMembershipUseCaseImpl(
                memberRepository = this.memberRepository
            ),

            // 멤버 차단/금지
            blockMemberUseCase = BlockMemberUseCaseImpl(
                projectRepository = this.projectRepository
            ),

            // 멤버 차단 해제
            unblockMemberUseCase = UnblockMemberUseCaseImpl(
                projectRepository = this.projectRepository
            ),

            // 차단된 멤버 목록 조회
            getBlockedProjectMembersUseCase = GetBlockedProjectMembersUseCaseImpl(
                memberRepository = this.memberRepository
            ),

            // 차단된 멤버 목록 실시간 관찰
            observeBlockedProjectMembersUseCase = ObserveBlockedProjectMembersUseCaseImpl(
                memberRepository = this.memberRepository
            ),

            // 멤버십 검증
            verifyProjectMembershipUseCase = VerifyProjectMembershipUseCaseImpl(
                memberRepository = this.memberRepository,
                authRepository = this.authRepository
            ),

            // OWNER helper
            isCurrentUserOwnerUseCase = IsCurrentUserOwnerUseCaseImpl(
                authRepository = this.authRepository,
                projectRepository = this.projectRepository
            ),

            // 오너 보호 및 권한 관련 UseCase들
            isTargetMemberOwnerUseCase = IsTargetMemberOwnerUseCaseImpl(
                projectRepository = this.projectRepository
            ),

            canManageTargetMemberUseCase = CanManageTargetMemberUseCaseImpl(
                authRepository = this.authRepository,
                memberRepository = this.memberRepository,
                projectRepository = this.projectRepository,
                projectRoleRepository = this.projectRoleRepository
            ),

            hasProjectPermissionUseCase = HasProjectPermissionUseCaseImpl(
                memberRepository = this.memberRepository,
                projectRoleRepository = this.projectRoleRepository
            ),

            ownerOrPermissionUseCase = OwnerOrPermissionUseCaseImpl(
                authRepository = this.authRepository,
                memberRepository = this.memberRepository,
                projectRoleRepository = this.projectRoleRepository
            ),

            getUserPermissionsForProjectUseCase = GetUserPermissionsForProjectUseCaseImpl(
                authRepository = this.authRepository,
                memberRepository = this.memberRepository,
                projectRoleRepository = this.projectRoleRepository
            ),

            getUserRolesForProjectUseCase = GetUserRolesForProjectUseCaseImpl(
                authRepository = this.authRepository,
                memberRepository = this.memberRepository
            ),

            permissionDeniedMessageUseCase = PermissionDeniedMessageUseCaseImpl()
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 멤버 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: DocumentId): ProjectMemberUseCases {
        return createForProject(projectId)
    }

    /**
     * 특정 멤버에 대한 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param memberId 멤버 ID
     * @return 멤버별 UseCase 그룹
     */
    fun createForMember(projectId: DocumentId, memberId: String): ProjectMemberUseCases {
        return createForProject(projectId)
    }
}

/**
 * 프로젝트 멤버 관리 UseCase 그룹
 */
data class ProjectMemberUseCases(
    // 멤버 기본 CRUD
    val addProjectMemberUseCase: AddProjectMemberUseCase,
    val getProjectMemberUseCase: GetProjectMemberUseCase,
    val getProjectMembersUseCase: GetProjectMembersUseCase,
    val getRoleMemberCountUseCase: GetRoleMemberCountUseCase,
    val removeProjectMemberUseCase: RemoveProjectMemberUseCase,
    
    // 멤버 고급 관리
    val getProjectMemberDetailsUseCase: GetProjectMemberDetailsUseCase,
    val deleteProjectMemberUseCase: DeleteProjectMemberUseCase,
    val removeMemberUseCase: RemoveMemberUseCase,
    val observeProjectMembersUseCase: ObserveProjectMembersUseCase,
    
    // 멤버 역할 관리
    val updateMemberRolesUseCase: UpdateMemberRolesUseCase,
    
    // 프로젝트 나가기 및 소유권 전달
    val leaveProjectUseCase: LeaveProjectUseCase,
    val transferOwnershipUseCase: TransferOwnershipUseCase,

    // 프로젝트 참여 (projectId 기반)
    val joinProjectByIdUseCase: JoinProjectByIdUseCase,

    // 프로젝트 초대 메시지 전송
    val sendProjectInviteMessageUseCase: SendProjectInviteMessageUseCase,

    // 프로젝트 초대 관리
    val sendProjectInvitationUseCase: SendProjectInvitationUseCase,
    val acceptProjectInvitationUseCase: AcceptProjectInvitationUseCase,
    val acceptProjectInviteFromMessageUseCase: AcceptProjectInviteFromMessageUseCase,

    // ViewModel 오케스트레이션용 DM 유틸 UseCase들
    val addDmChannelUseCase: AddDmChannelUseCase,
    val getDmChannelUseCase: GetDmChannelUseCase,

    // 멤버십 확인
    val checkUserProjectMembershipUseCase: CheckUserProjectMembershipUseCase,

    // 멤버 차단/금지
    val blockMemberUseCase: BlockMemberUseCase,

    // 멤버 차단 해제
    val unblockMemberUseCase: UnblockMemberUseCase,

    // 차단된 멤버 목록 조회
    val getBlockedProjectMembersUseCase: GetBlockedProjectMembersUseCase,

    // 차단된 멤버 목록 실시간 관찰
    val observeBlockedProjectMembersUseCase: ObserveBlockedProjectMembersUseCase,

    // 멤버십 검증
    val verifyProjectMembershipUseCase: VerifyProjectMembershipUseCase,

    // OWNER helper
    val isCurrentUserOwnerUseCase: IsCurrentUserOwnerUseCase,

    // 오너 보호 및 권한 관련 UseCase들
    val isTargetMemberOwnerUseCase: IsTargetMemberOwnerUseCase,
    val canManageTargetMemberUseCase: CanManageTargetMemberUseCase,
    val hasProjectPermissionUseCase: HasProjectPermissionUseCase,
    val ownerOrPermissionUseCase: OwnerOrPermissionUseCase,
    val getUserPermissionsForProjectUseCase: GetUserPermissionsForProjectUseCase,
    val getUserRolesForProjectUseCase: GetUserRolesForProjectUseCase,
    val permissionDeniedMessageUseCase: PermissionDeniedMessageUseCase
)
