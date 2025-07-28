package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId

import com.example.domain.repository.remote.AuthRepository
import com.example.domain.usecase.project.role.CreateProjectRoleUseCase
import com.example.domain.usecase.project.role.CreateProjectRoleUseCaseImpl
import com.example.domain.usecase.project.role.CreateRoleUseCase
import com.example.domain.usecase.project.role.CreateRoleUseCaseImpl
import com.example.domain.usecase.project.role.DeleteRoleUseCase
import com.example.domain.usecase.project.role.DeleteRoleUseCaseImpl
import com.example.domain.usecase.project.role.GetProjectRoleUseCase
import com.example.domain.usecase.project.role.GetProjectRoleUseCaseImpl
import com.example.domain.usecase.project.role.GetProjectRolesUseCase
import com.example.domain.usecase.project.role.GetProjectRolesUseCaseImpl
import com.example.domain.usecase.project.role.GetRoleDetailsUseCase
import com.example.domain.usecase.project.role.GetRoleDetailsUseCaseImpl
import com.example.domain.usecase.project.role.GetRolePermissionsUseCase
import com.example.domain.usecase.project.role.GetRolePermissionsUseCaseImpl
import com.example.domain.usecase.project.role.UpdateProjectRoleUseCase
import com.example.domain.usecase.project.role.UpdateProjectRoleUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.model.base.Role
import com.example.domain.model.base.Permission

/**
 * 프로젝트 역할 관리 UseCase들을 제공하는 Provider
 * 
 * 역할 생성, 삭제, 수정, 권한 관리 등의 역할 관련 기능을 담당합니다.
 */
@Singleton
class ProjectRoleUseCaseProvider @Inject constructor(
    private val projectRoleRepository: DefaultRepository<Role>,
    private val permissionRepository: DefaultRepository<Permission>,
    private val authRepository: AuthRepository
) {

    /**
     * 특정 프로젝트의 역할 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 역할 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectRoleUseCases {
        projectRoleRepository.setCollection(CollectionPath.projectRoles(projectId.value))
        permissionRepository.setCollection(CollectionPath.projectRolePermissions(projectId.value, ""))

        return ProjectRoleUseCases(
            // 역할 기본 CRUD
            createRoleUseCase = CreateRoleUseCaseImpl(
                projectRoleRepository = projectRoleRepository
            ),
            
            createProjectRoleUseCase = CreateProjectRoleUseCaseImpl(
                projectRoleRepository = projectRoleRepository
            ),
            
            updateProjectRoleUseCase = UpdateProjectRoleUseCaseImpl(
                projectRoleRepository = projectRoleRepository
            ),
            
            deleteRoleUseCase = DeleteRoleUseCaseImpl(
                projectRoleRepository = projectRoleRepository,
                authRepository = authRepository
            ),
            
            // 역할 조회
            getProjectRoleUseCase = GetProjectRoleUseCaseImpl(
                projectRoleRepository = projectRoleRepository
            ),
            
            getProjectRolesUseCase = GetProjectRolesUseCaseImpl(
                projectRoleRepository = projectRoleRepository
            ),
            
            getRoleDetailsUseCase = GetRoleDetailsUseCaseImpl(
                projectRoleRepository = projectRoleRepository
            ),
            
            // 권한 관리
            getRolePermissionsUseCase = GetRolePermissionsUseCaseImpl(
                permissionRepository = permissionRepository
            ),

            projectRoleRepository= projectRoleRepository,
            permissionRepository= permissionRepository,
            authRepository= authRepository
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 역할 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 역할 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: DocumentId): ProjectRoleUseCases {
        return createForProject(projectId)
    }

    /**
     * 특정 역할에 대한 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @return 역할별 UseCase 그룹
     */
    fun createForRole(projectId: DocumentId, roleId: DocumentId): ProjectRoleUseCases {
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

    val projectRoleRepository: DefaultRepository<Role>,
    val permissionRepository: DefaultRepository<Permission>,
    val authRepository: AuthRepository
)