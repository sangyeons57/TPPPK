package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.ProjectRoleRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRoleRepositoryFactoryContext
import com.example.domain.usecase.project.role.DeleteRoleUseCase
import com.example.domain.usecase.project.role.CreateProjectRoleUseCase
import com.example.domain.usecase.project.role.CreateRoleUseCase
import com.example.domain.usecase.project.role.GetProjectRoleUseCase
import com.example.domain.usecase.project.role.GetProjectRolesUseCase
import com.example.domain.usecase.project.role.GetRoleDetailsUseCase
import com.example.domain.usecase.project.role.GetRolePermissionsUseCase
import com.example.domain.usecase.project.role.UpdateProjectRoleUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 역할 관리 UseCase들을 제공하는 Provider
 * 
 * 역할 생성, 삭제, 수정, 권한 관리 등의 역할 관련 기능을 담당합니다.
 */
@Singleton
class ProjectRoleUseCaseProvider @Inject constructor(
    private val projectProjectRoleRepositoryFactory: RepositoryFactory<ProjectRoleRepositoryFactoryContext, ProjectRoleRepository>,
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 프로젝트의 역할 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 역할 관리 UseCase 그룹
     */
    fun createForProject(projectId: String): ProjectRoleUseCases {
        val roleRepository = projectProjectRoleRepositoryFactory.create(
            ProjectRoleRepositoryFactoryContext(
                collectionPath = CollectionPath.projectRoles(projectId)
            )
        )

        val projectRoleRepository = projectProjectRoleRepositoryFactory.create(
            ProjectRoleRepositoryFactoryContext(
                collectionPath = CollectionPath.projectRoles(projectId)
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return ProjectRoleUseCases(
            // 역할 기본 CRUD
            createRoleUseCase = CreateRoleUseCase(
                roleRepository = roleRepository,
                authRepository = authRepository
            ),
            
            createProjectRoleUseCase = CreateProjectRoleUseCase(
                projectRoleRepository = projectRoleRepository,
                authRepository = authRepository
            ),
            
            updateProjectRoleUseCase = UpdateProjectRoleUseCase(
                projectRoleRepository = projectRoleRepository,
                authRepository = authRepository
            ),
            
            deleteRoleUseCase = DeleteRoleUseCase(
                roleRepository = roleRepository
            ),
            
            // 역할 조회
            getProjectRoleUseCase = GetProjectRoleUseCase(
                projectRoleRepository = projectRoleRepository
            ),
            
            getProjectRolesUseCase = GetProjectRolesUseCase(
                projectRoleRepository = projectRoleRepository
            ),
            
            getRoleDetailsUseCase = GetRoleDetailsUseCase(
                roleRepository = roleRepository
            ),
            
            // 권한 관리
            getRolePermissionsUseCase = GetRolePermissionsUseCase(
                roleRepository = roleRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            roleRepository = roleRepository,
            projectRoleRepository = projectRoleRepository
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 역할 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 역할 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: String): ProjectRoleUseCases {
        return createForProject(projectId)
    }

    /**
     * 특정 역할에 대한 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @return 역할별 UseCase 그룹
     */
    fun createForRole(projectId: String, roleId: String): ProjectRoleUseCases {
        return createForProject(projectId)
    }
}

/**
 * 프로젝트 역할 관리 UseCase 그룹
 */
data class ProjectRoleUseCases(
    // 역할 기본 CRUD
    val createRoleUseCase: CreateRoleUseCase,
    val createProjectRoleUseCase: CreateProjectRoleUseCase,
    val updateProjectRoleUseCase: UpdateProjectRoleUseCase,
    val deleteRoleUseCase: DeleteRoleUseCase,
    
    // 역할 조회
    val getProjectRoleUseCase: GetProjectRoleUseCase,
    val getProjectRolesUseCase: GetProjectRolesUseCase,
    val getRoleDetailsUseCase: GetRoleDetailsUseCase,
    
    // 권한 관리
    val getRolePermissionsUseCase: GetRolePermissionsUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val roleRepository: ProjectRoleRepository,
    val projectRoleRepository: ProjectRoleRepository
)