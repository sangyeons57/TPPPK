package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import com.example.domain.usecase.project.DeleteProjectMemberUseCase
import com.example.domain.usecase.project.DeleteProjectMemberUseCaseImpl
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCase
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCaseImpl
import com.example.domain.usecase.project.ObserveProjectMembersUseCase
import com.example.domain.usecase.project.ObserveProjectMembersUseCaseImpl
import com.example.domain.usecase.project.member.AddProjectMemberUseCase
import com.example.domain.usecase.project.member.AddProjectMemberUseCaseImpl
import com.example.domain.usecase.project.member.GetProjectMemberUseCase
import com.example.domain.usecase.project.member.GetProjectMemberUseCaseImpl
import com.example.domain.usecase.project.member.RemoveProjectMemberUseCase
import com.example.domain.usecase.project.member.RemoveProjectMemberUseCaseImpl
import com.example.domain.usecase.project.member.UpdateMemberRolesUseCase
import com.example.domain.usecase.project.member.UpdateMemberRolesUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 멤버 관리 UseCase들을 제공하는 Provider
 * 
 * 멤버 추가, 제거, 조회, 역할 관리 등의 멤버 관련 기능을 담당합니다.
 */
@Singleton
class ProjectMemberUseCaseProvider @Inject constructor(
    private val memberRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MemberRepositoryFactoryContext, MemberRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 프로젝트의 멤버 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectMemberUseCases {
        val memberRepository = memberRepositoryFactory.create(
            MemberRepositoryFactoryContext(
                collectionPath = CollectionPath.projectMembers(projectId.value)
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return ProjectMemberUseCases(
            // 멤버 기본 CRUD
            addProjectMemberUseCase = AddProjectMemberUseCaseImpl(
                projectMemberRepository = memberRepository
            ),
            
            getProjectMemberUseCase = GetProjectMemberUseCaseImpl(
                projectMemberRepository = memberRepository
            ),
            
            removeProjectMemberUseCase = RemoveProjectMemberUseCaseImpl(
                projectMemberRepository = memberRepository
            ),
            
            // 멤버 고급 관리
            getProjectMemberDetailsUseCase = GetProjectMemberDetailsUseCaseImpl(
                projectMemberRepository = memberRepository
            ),
            
            deleteProjectMemberUseCase = DeleteProjectMemberUseCaseImpl(
                projectMemberRepository = memberRepository
            ),
            
            observeProjectMembersUseCase = ObserveProjectMembersUseCaseImpl(
                projectMemberRepository = memberRepository
            ),
            
            // 멤버 역할 관리
            updateMemberRolesUseCase = UpdateMemberRolesUseCaseImpl(
                memberRepository = memberRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            memberRepository = memberRepository
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
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val memberRepository: MemberRepository
)