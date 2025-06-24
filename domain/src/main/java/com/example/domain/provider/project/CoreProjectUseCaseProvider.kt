package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.base.ProjectsWrapperRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectsWrapperRepositoryFactoryContext
import com.example.domain.usecase.project.core.CreateProjectUseCase
import com.example.domain.usecase.project.core.DeleteProjectUseCase
import com.example.domain.usecase.project.core.GetProjectDetailsStreamUseCase
import com.example.domain.usecase.project.core.GetUserParticipatingProjectsUseCase
import com.example.domain.usecase.project.core.JoinProjectWithCodeUseCase
import com.example.domain.usecase.project.core.JoinProjectWithTokenUseCase
import com.example.domain.usecase.project.core.RenameProjectUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 핵심 프로젝트 관리 UseCase들을 제공하는 Provider
 * 
 * 프로젝트 생성, 삭제, 이름 변경, 조회, 참여 등의 기본 CRUD 기능을 담당합니다.
 */
@Singleton
class CoreProjectUseCaseProvider @Inject constructor(
    private val projectRepositoryFactory: RepositoryFactory<ProjectRepositoryFactoryContext, ProjectRepository>,
    private val projectsWrapperRepositoryFactory: RepositoryFactory<ProjectsWrapperRepositoryFactoryContext, ProjectsWrapperRepository>,
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 프로젝트에 대한 핵심 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID (선택적)
     * @param userId 사용자 ID (선택적)
     * @return 핵심 프로젝트 관리 UseCase 그룹
     */
    fun createForProject(projectId: String? = null, userId: String? = null): CoreProjectUseCases {
        val projectRepository = projectRepositoryFactory.create(
            ProjectRepositoryFactoryContext(
                collectionPath = CollectionPath.projects
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        val projectsWrapperRepository = if (userId != null) {
            projectsWrapperRepositoryFactory.create(
                ProjectsWrapperRepositoryFactoryContext(
                    collectionPath = CollectionPath.userProjectWrappers(userId)
                )
            )
        } else null

        return CoreProjectUseCases(
            createProjectUseCase = CreateProjectUseCase(
                projectRepository = projectRepository,
                projectsWrapperRepository = projectsWrapperRepository 
                    ?: error("프로젝트 생성에는 사용자 ID가 필요합니다"),
                authRepository = authRepository,
                categoryRepository = null, // CategoryRepository는 별도 Provider에서 관리
                memberRepository = null    // MemberRepository는 별도 Provider에서 관리
            ),
            
            deleteProjectUseCase = DeleteProjectUseCase(
                projectRepository = projectRepository,
                projectsWrapperRepository = projectsWrapperRepository
                    ?: error("프로젝트 삭제에는 사용자 ID가 필요합니다")
            ),
            
            renameProjectUseCase = RenameProjectUseCase(
                projectRepository = projectRepository
            ),
            
            getProjectDetailsStreamUseCase = GetProjectDetailsStreamUseCase(
                projectRepository = projectRepository
            ),
            
            getUserParticipatingProjectsUseCase = GetUserParticipatingProjectsUseCase(
                projectsWrapperRepository = projectsWrapperRepository
                    ?: error("사용자 프로젝트 조회에는 사용자 ID가 필요합니다"),
                projectRepository = projectRepository
            ),
            
            joinProjectWithCodeUseCase = JoinProjectWithCodeUseCase(
                projectRepository = projectRepository
            ),
            
            joinProjectWithTokenUseCase = JoinProjectWithTokenUseCase(
                projectRepository = projectRepository
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

        val projectRepository = projectRepositoryFactory.create(
            ProjectRepositoryFactoryContext(
                collectionPath = CollectionPath.projects
            )
        )

        // 현재 사용자 기반으로 ProjectsWrapperRepository 생성
        // Note: 현재 사용자 ID가 필요하므로 실제로는 createForProject를 사용해야 함
        val projectsWrapperRepository = projectsWrapperRepositoryFactory.create(
            ProjectsWrapperRepositoryFactoryContext(
                collectionPath = CollectionPath.projects // 임시 경로, 실제 사용 시 userId 필요
            )
        )

        return CoreProjectUseCases(
            createProjectUseCase = CreateProjectUseCase(
                projectRepository = projectRepository,
                projectsWrapperRepository = projectsWrapperRepository,
                authRepository = authRepository,
                categoryRepository = null,
                memberRepository = null
            ),
            
            deleteProjectUseCase = DeleteProjectUseCase(
                projectRepository = projectRepository,
                projectsWrapperRepository = projectsWrapperRepository
            ),
            
            renameProjectUseCase = RenameProjectUseCase(
                projectRepository = projectRepository
            ),
            
            getProjectDetailsStreamUseCase = GetProjectDetailsStreamUseCase(
                projectRepository = projectRepository
            ),
            
            getUserParticipatingProjectsUseCase = GetUserParticipatingProjectsUseCase(
                projectsWrapperRepository = projectsWrapperRepository,
                projectRepository = projectRepository
            ),
            
            joinProjectWithCodeUseCase = JoinProjectWithCodeUseCase(
                projectRepository = projectRepository
            ),
            
            joinProjectWithTokenUseCase = JoinProjectWithTokenUseCase(
                projectRepository = projectRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            projectRepository = projectRepository
        )
    }
}

/**
 * 핵심 프로젝트 관리 UseCase 그룹
 */
data class CoreProjectUseCases(
    val createProjectUseCase: CreateProjectUseCase,
    val deleteProjectUseCase: DeleteProjectUseCase,
    val renameProjectUseCase: RenameProjectUseCase,
    val getProjectDetailsStreamUseCase: GetProjectDetailsStreamUseCase,
    val getUserParticipatingProjectsUseCase: GetUserParticipatingProjectsUseCase,
    val joinProjectWithCodeUseCase: JoinProjectWithCodeUseCase,
    val joinProjectWithTokenUseCase: JoinProjectWithTokenUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val projectRepository: ProjectRepository
)