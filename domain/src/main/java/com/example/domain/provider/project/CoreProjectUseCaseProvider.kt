package com.example.domain.provider.project

import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Initial.getOrThrow
import com.example.core_common.result.exceptionOrNull
import com.example.core_common.result.getOrNull
import com.example.domain.model.base.Category
import com.example.domain.model.base.Member
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.Role
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.repository.remote.ProjectInvitationRepository
import com.example.domain.usecase.project.JoinProjectWithCodeUseCase
import com.example.domain.usecase.project.core.JoinProjectWithTokenUseCase
import com.example.domain.usecase.project.core.CreateProjectUseCase
import com.example.domain.usecase.project.core.DeleteProjectUseCase
import com.example.domain.usecase.project.core.DeleteProjectUseCaseImpl
import com.example.domain.usecase.project.core.DeleteProjectsWrapperUseCase
import com.example.domain.usecase.project.core.DeleteProjectsWrapperUseCaseImpl
import com.example.domain.usecase.project.core.GenerateInviteLinkUseCase
import com.example.domain.usecase.project.core.GenerateInviteLinkFromIdUseCase
import com.example.domain.usecase.project.core.GetProjectDetailsStreamUseCase
import com.example.domain.usecase.project.core.GetUserParticipatingProjectsUseCaseImpl
import com.example.domain.usecase.project.core.RenameProjectUseCaseImpl
import com.example.domain.usecase.project.core.ValidateInviteCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 핵심 프로젝트 관리 UseCase들을 제공하는 Provider
 * 
 * 프로젝트 생성, 삭제, 이름 변경, 조회, 참여 등의 기본 CRUD 기능을 담당합니다.
 */
@Singleton
class CoreProjectUseCaseProvider @Inject constructor(
    private val projectRepository: DefaultRepository<Project>,
    private val projectsWrapperRepository: DefaultRepository<ProjectsWrapper>,
    private val authRepository: AuthRepository,
    private val categoryRepository: DefaultRepository<Category>,
    private val memberRepository: DefaultRepository<Member>,
    private val roleRepository: DefaultRepository<Role>,
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
        projectRepository.setCollection(CollectionPath.projects)
        projectsWrapperRepository.setCollection(CollectionPath.userProjectWrappers(userId.value))
        categoryRepository.setCollection(CollectionPath.projectCategories(projectId.value))
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))
        roleRepository.setCollection(CollectionPath.projectRoles(projectId.value))

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
            ),
            projectRepository = projectRepository,
            projectsWrapperRepository = projectsWrapperRepository,
            authRepository = authRepository,
            categoryRepository = categoryRepository,
            memberRepository = memberRepository,
            roleRepository = roleRepository,
            projectInvitationRepository = projectInvitationRepository
        )
    }

    /**
     * 현재 사용자를 위한 핵심 UseCase들을 생성합니다.
     * 
     * @return 사용자별 핵심 프로젝트 관리 UseCase 그룹
     */
    suspend fun createForCurrentUser(): CoreProjectUseCases {

        val session = authRepository.getCurrentUserSession().getOrThrow()

        projectRepository.setCollection(CollectionPath.projects)

        // 현재 사용자 기반으로 ProjectsWrapperRepository 생성
        // Note: 현재 사용자 ID가 필요하므로 실제로는 createForProject를 사용해야 함
        projectsWrapperRepository.setCollection(CollectionPath.userProjectWrappers(session.userId.value))

        // 임시로 "temp-project" ID 사용
        categoryRepository.setCollection(CollectionPath.projectCategories("temp-project"))

        memberRepository.setCollection(CollectionPath.projectMembers("temp-project"))

        roleRepository.setCollection(CollectionPath.projectRoles("temp-project"))

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
            generateInviteLinkFromIdUseCase = GenerateInviteLinkFromIdUseCase(),
            projectRepository = projectRepository,
            projectsWrapperRepository = projectsWrapperRepository,
            authRepository = authRepository,
            categoryRepository = categoryRepository,
            memberRepository = memberRepository,
            roleRepository = roleRepository,
            projectInvitationRepository = projectInvitationRepository
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
    val generateInviteLinkFromIdUseCase : GenerateInviteLinkFromIdUseCase,
    val projectRepository: DefaultRepository<Project>,
    val projectsWrapperRepository: DefaultRepository<ProjectsWrapper>,
    val authRepository: AuthRepository,
    val categoryRepository: DefaultRepository<Category>,
    val memberRepository: DefaultRepository<Member>,
    val roleRepository: DefaultRepository<Role>,
    val projectInvitationRepository: ProjectInvitationRepository
)