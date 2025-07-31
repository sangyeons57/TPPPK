package com.example.domain_usecase.provider.project

import com.example.domain.model.vo.DocumentId
import com.example.domain.usecase.project.DeleteProjectMemberUseCase
import com.example.domain.usecase.project.DeleteProjectMemberUseCaseImpl
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCase
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCaseImpl
import com.example.domain.usecase.project.ObserveProjectMembersUseCase
import com.example.domain.usecase.project.ObserveProjectMembersUseCaseImpl
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_usecase.usecase.project.member.AddProjectMemberUseCase
import com.example.domain_usecase.usecase.project.member.AddProjectMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetProjectMemberUseCase
import com.example.domain_usecase.usecase.project.member.GetProjectMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.LeaveProjectUseCase
import com.example.domain_usecase.usecase.project.member.LeaveProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.member.RemoveProjectMemberUseCase
import com.example.domain_usecase.usecase.project.member.RemoveProjectMemberUseCaseImpl
import com.example.domain_usecase.usecase.project.member.TransferOwnershipUseCase
import com.example.domain_usecase.usecase.project.member.TransferOwnershipUseCaseImpl
import com.example.domain_usecase.usecase.project.member.UpdateMemberRolesUseCase
import com.example.domain_usecase.usecase.project.member.UpdateMemberRolesUseCaseImpl
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
    private val projectRepository: ProjectRepository
) {

    /**
     * 특정 프로젝트의 멤버 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectMemberUseCases {
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        projectRepository.setCollection(CollectionPath.project(projectId.value))

        return ProjectMemberUseCases(
            // 멤버 기본 CRUD
            addProjectMemberUseCase = AddProjectMemberUseCaseImpl(
                projectMemberRepository = this.memberRepository
            ),
            
            getProjectMemberUseCase = GetProjectMemberUseCaseImpl(
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
            )
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
    val removeProjectMemberUseCase: RemoveProjectMemberUseCase,
    
    // 멤버 고급 관리
    val getProjectMemberDetailsUseCase: GetProjectMemberDetailsUseCase,
    val deleteProjectMemberUseCase: DeleteProjectMemberUseCase,
    val observeProjectMembersUseCase: ObserveProjectMembersUseCase,
    
    // 멤버 역할 관리
    val updateMemberRolesUseCase: UpdateMemberRolesUseCase,
    
    // 프로젝트 나가기 및 소유권 전달
    val leaveProjectUseCase: LeaveProjectUseCase,
    val transferOwnershipUseCase: TransferOwnershipUseCase
)