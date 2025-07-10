package com.example.domain.provider.project

import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Initial.getOrThrow
import com.example.core_common.result.exceptionOrNull
import com.example.core_common.result.getOrNull
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.base.ProjectInvitationRepository
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectRoleRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectInvitationRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
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
    private val projectRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectRepositoryFactoryContext, ProjectRepository>,
    private val projectsWrapperRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectsWrapperRepositoryFactoryContext, ProjectsWrapperRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val categoryRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>,
    private val memberRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MemberRepositoryFactoryContext, MemberRepository>,
    private val roleRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectRoleRepositoryFactoryContext, ProjectRoleRepository>,
    private val projectInvitationRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectInvitationRepositoryFactoryContext, ProjectInvitationRepository>
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
        val projectRepository = projectRepositoryFactory.create(
            ProjectRepositoryFactoryContext(
                collectionPath = CollectionPath.projects
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        val projectsWrapperRepository = projectsWrapperRepositoryFactory.create(
                ProjectsWrapperRepositoryFactoryContext(
                    collectionPath = CollectionPath.userProjectWrappers(userId.value)
                )
            )

        val categoryRepository = categoryRepositoryFactory.create(
                CategoryRepositoryFactoryContext(
                    collectionPath = CollectionPath.projectCategories(projectId.value)
                )
            )

        val memberRepository = memberRepositoryFactory.create(
                MemberRepositoryFactoryContext(
                    collectionPath = CollectionPath.projectMembers(projectId.value)
                )
            )

        val roleRepository = roleRepositoryFactory.create(
            ProjectRoleRepositoryFactoryContext(
                collectionPath = CollectionPath.projectRoles(projectId.value)
            )
        )

        val projectInvitationRepository = projectInvitationRepositoryFactory.create(
            ProjectInvitationRepositoryFactoryContext()
        )

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
            
            // 공통 Repository
            authRepository = authRepository,
            projectRepository = projectRepository
        )
    }

    /**
     * 현재 사용자를 위한 핵심 UseCase들을 생성합니다.
     * 
     * @return 사용자별 핵심 프로젝트 관리 UseCase 그룹
     */
    fun createForCurrentUser(): CoreProjectUseCases {
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        val session = authRepository.getCurrentUserSession().getOrThrow()

        val projectRepository = projectRepositoryFactory.create(
            ProjectRepositoryFactoryContext(
                collectionPath = CollectionPath.projects
            )
        )

        // 현재 사용자 기반으로 ProjectsWrapperRepository 생성
        // Note: 현재 사용자 ID가 필요하므로 실제로는 createForProject를 사용해야 함
        val projectsWrapperRepository = projectsWrapperRepositoryFactory.create(
            ProjectsWrapperRepositoryFactoryContext(
                collectionPath = CollectionPath.userProjectWrappers(session.userId.value)
            )
        )

        // 임시로 "temp-project" ID 사용
        val categoryRepository = categoryRepositoryFactory.create(
            CategoryRepositoryFactoryContext(
                collectionPath = CollectionPath.projectCategories("temp-project")
            )
        )

        val memberRepository = memberRepositoryFactory.create(
            MemberRepositoryFactoryContext(
                collectionPath = CollectionPath.projectMembers("temp-project")
            )
        )

        val roleRepository = roleRepositoryFactory.create(
            ProjectRoleRepositoryFactoryContext(
                collectionPath = CollectionPath.projectRoles("temp-project")
            )
        )

        val projectInvitationRepository = projectInvitationRepositoryFactory.create(
            ProjectInvitationRepositoryFactoryContext()
        )

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

            // 공통 Repository
            authRepository = authRepository,
            projectRepository = projectRepository,
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
    // 공통 Repository
    val authRepository: AuthRepository,
    val projectRepository: ProjectRepository
)