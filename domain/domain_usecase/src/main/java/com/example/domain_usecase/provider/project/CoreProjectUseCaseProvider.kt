package com.example.domain_usecase.provider.project

import com.example.core_common.result.CustomResult.Initial.getOrThrow
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.CategoryRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectInvitationRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.domain_repository.base.ProjectsWrapperRepository
import com.example.domain_usecase.usecase.project.JoinProjectWithCodeUseCase
import com.example.domain_usecase.usecase.project.core.CreateProjectUseCase
import com.example.domain_usecase.usecase.project.core.DeleteProjectUseCase
import com.example.domain_usecase.usecase.project.core.DeleteProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.core.DeleteProjectsWrapperUseCase
import com.example.domain_usecase.usecase.project.core.DeleteProjectsWrapperUseCaseImpl
import com.example.domain_usecase.usecase.project.core.GenerateInviteLinkFromIdUseCase
import com.example.domain_usecase.usecase.project.core.GenerateInviteLinkUseCase
import com.example.domain_usecase.usecase.project.core.GetProjectDetailsStreamUseCase
import com.example.domain_usecase.usecase.project.core.GetUserParticipatingProjectsUseCaseImpl
import com.example.domain_usecase.usecase.project.core.JoinProjectWithTokenUseCase
import com.example.domain_usecase.usecase.project.core.RenameProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.core.ValidateInviteCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 핵심 프로젝트 관리 UseCase들을 제공하는 Provider
 * 
 * 프로젝트 생성, 삭제, 이름 변경, 조회, 참여 등의 기본 CRUD 기능을 담당합니다.
 */
@Singleton
class CoreProjectUseCaseProvider @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectsWrapperRepository: ProjectsWrapperRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository,
    private val memberRepository: MemberRepository,
    private val roleRepository: ProjectRoleRepository,
    private val projectInvitationRepository: ProjectInvitationRepository
) {

    /**
     * 특정 프로젝트에 대한 핵심 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID (선택적)
     * @param userId 사용자 ID (선택적)
     * @return 핵심 프로젝트 관리 UseCase 그룹
     */
    fun createForProject(
        projectId: DocumentId,
        userId: UserId
    ): CoreProjectUseCases {
        // Set collection paths for each repository
        projectRepository.setCollection(CollectionPath.projects)
        projectsWrapperRepository.setCollection(CollectionPath.userProjectWrappers(userId.value))
        categoryRepository.setCollection(CollectionPath.projectCategories(projectId.value))
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        roleRepository.setCollection(CollectionPath.projectRoles(projectId.value))
        projectInvitationRepository.setCollection(CollectionPath.projectInvitations())

        return CoreProjectUseCases(
            createProjectUseCase = CreateProjectUseCase(
                projectRepository = projectRepository,
                projectsWrapperRepository = projectsWrapperRepository,
                authRepository = authRepository,
                categoryRepository = categoryRepository,
                memberRepository = memberRepository,
                roleRepository = roleRepository
            ),
            
            deleteProjectUseCase = DeleteProjectUseCaseImpl(
                projectRepository = projectRepository,
                authRepository = authRepository,
                projWrapperRepository = projectsWrapperRepository
            ),
            
            renameProjectUseCase = RenameProjectUseCaseImpl(
                projectRepository = projectRepository
            ),
            
            getProjectDetailsStreamUseCase = GetProjectDetailsStreamUseCase(
                projectRepository = projectRepository
            ),
            
            getUserParticipatingProjectsUseCase = GetUserParticipatingProjectsUseCaseImpl(
                projectsWrapperRepository = projectsWrapperRepository,
                projectRepository = projectRepository
            ),
            
            joinProjectWithCodeUseCase = JoinProjectWithCodeUseCase(
                projectInvitationRepository = projectInvitationRepository
            ),
            
            joinProjectWithTokenUseCase = JoinProjectWithTokenUseCase(
                projectRepository = projectRepository
            ),
            
            generateInviteLinkUseCase = GenerateInviteLinkUseCase(
                projectInvitationRepository = projectInvitationRepository
            ),

            generateInviteLinkFromIdUseCase = GenerateInviteLinkFromIdUseCase(),

            validateInviteCodeUseCase = ValidateInviteCodeUseCase(
                projectInvitationRepository = projectInvitationRepository
            ),
            
            deleteProjectsWrapperUseCase = DeleteProjectsWrapperUseCaseImpl(
                projectsWrapperRepository = projectsWrapperRepository
            )
        )
    }

    /**
     * 현재 사용자를 위한 핵심 UseCase들을 생성합니다.
     * 
     * @return 사용자별 핵심 프로젝트 관리 UseCase 그룹
     */
    suspend fun createForCurrentUser(): CoreProjectUseCases {
        val session = authRepository.getCurrentUserSession().getOrThrow()

        // Set collection paths for each repository
        projectRepository.setCollection(CollectionPath.projects)
        projectsWrapperRepository.setCollection(CollectionPath.userProjectWrappers(session.userId.value))
        
        // 임시로 "temp-project" ID 사용
        categoryRepository.setCollection(CollectionPath.projectCategories("temp-project"))
        memberRepository.setCollection(CollectionPath.projectMembers("temp-project"))
        roleRepository.setCollection(CollectionPath.projectRoles("temp-project"))
        projectInvitationRepository.setCollection(CollectionPath.projectInvitations())

        return CoreProjectUseCases(
            createProjectUseCase = CreateProjectUseCase(
                projectRepository = projectRepository,
                projectsWrapperRepository = projectsWrapperRepository,
                authRepository = authRepository,
                categoryRepository = categoryRepository,
                memberRepository = memberRepository,
                roleRepository = roleRepository
            ),

            deleteProjectUseCase = DeleteProjectUseCaseImpl(
                projectRepository = projectRepository,
                authRepository = authRepository,
                projWrapperRepository = projectsWrapperRepository
            ),

            renameProjectUseCase = RenameProjectUseCaseImpl(
                projectRepository = projectRepository
            ),

            getProjectDetailsStreamUseCase = GetProjectDetailsStreamUseCase(
                projectRepository = projectRepository
            ),

            getUserParticipatingProjectsUseCase = GetUserParticipatingProjectsUseCaseImpl(
                projectsWrapperRepository = projectsWrapperRepository,
                projectRepository = projectRepository
            ),

            joinProjectWithCodeUseCase = JoinProjectWithCodeUseCase(
                projectInvitationRepository = projectInvitationRepository
            ),

            joinProjectWithTokenUseCase = JoinProjectWithTokenUseCase(
                projectRepository = projectRepository
            ),

            generateInviteLinkUseCase = GenerateInviteLinkUseCase(
                projectInvitationRepository = projectInvitationRepository
            ),

            validateInviteCodeUseCase = ValidateInviteCodeUseCase(
                projectInvitationRepository = projectInvitationRepository
            ),

            deleteProjectsWrapperUseCase = DeleteProjectsWrapperUseCaseImpl(
                projectsWrapperRepository = projectsWrapperRepository
            ),
            generateInviteLinkFromIdUseCase = GenerateInviteLinkFromIdUseCase()
        )
    }
}

/**
 * 핵심 프로젝트 관리 UseCase 그룹
 */
data class CoreProjectUseCases(
    val createProjectUseCase: CreateProjectUseCase,
    val deleteProjectUseCase: DeleteProjectUseCase,
    val renameProjectUseCase: RenameProjectUseCaseImpl,
    val getProjectDetailsStreamUseCase: GetProjectDetailsStreamUseCase,
    val getUserParticipatingProjectsUseCase: GetUserParticipatingProjectsUseCaseImpl,
    val joinProjectWithCodeUseCase: JoinProjectWithCodeUseCase,
    val joinProjectWithTokenUseCase: JoinProjectWithTokenUseCase,
    val generateInviteLinkUseCase: GenerateInviteLinkUseCase,
    val validateInviteCodeUseCase: ValidateInviteCodeUseCase,
    val deleteProjectsWrapperUseCase: DeleteProjectsWrapperUseCase,
    val generateInviteLinkFromIdUseCase : GenerateInviteLinkFromIdUseCase
)