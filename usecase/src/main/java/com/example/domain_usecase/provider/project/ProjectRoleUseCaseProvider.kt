package com.example.domain_usecase.provider.project

import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.PermissionRepository
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.domain_usecase.usecase.project.member.GetUserPermissionsForProjectUseCase
import com.example.domain_usecase.usecase.project.member.GetUserPermissionsForProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.member.GetUserRolesForProjectUseCase
import com.example.domain_usecase.usecase.project.member.GetUserRolesForProjectUseCaseImpl
import com.example.domain_usecase.usecase.project.role.CreateProjectRoleUseCase
import com.example.domain_usecase.usecase.project.role.CreateProjectRoleUseCaseImpl
import com.example.domain_usecase.usecase.project.role.CreateRoleUseCase
import com.example.domain_usecase.usecase.project.role.CreateRoleUseCaseImpl
import com.example.domain_usecase.usecase.project.role.DeleteRoleUseCase
import com.example.domain_usecase.usecase.project.role.DeleteRoleUseCaseImpl
import com.example.domain_usecase.usecase.project.role.GetProjectRoleUseCase
import com.example.domain_usecase.usecase.project.role.GetProjectRoleUseCaseImpl
import com.example.domain_usecase.usecase.project.role.GetProjectRolesUseCase
import com.example.domain_usecase.usecase.project.role.GetProjectRolesUseCaseImpl
import com.example.domain_usecase.usecase.project.role.GetRoleDetailsUseCase
import com.example.domain_usecase.usecase.project.role.GetRoleDetailsUseCaseImpl
import com.example.domain_usecase.usecase.project.role.GetRolePermissionsUseCase
import com.example.domain_usecase.usecase.project.role.GetRolePermissionsUseCaseImpl
import com.example.domain_usecase.usecase.project.role.SetRolePermissionsUseCase
import com.example.domain_usecase.usecase.project.role.UpdateProjectRoleUseCase
import com.example.domain_usecase.usecase.project.role.UpdateProjectRoleUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 역할 관리 UseCase들을 제공하는 Provider
 * 
 * 역할 생성, 삭제, 수정, 권한 관리 등의 역할 관련 기능을 담당합니다.
 */
@Singleton
class ProjectRoleUseCaseProvider @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository,
    private val memberRepository: MemberRepository,
    private val permissionRepository: PermissionRepository,
    private val authRepository: AuthRepository
) {

    /**
     * 특정 프로젝트의 역할 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 역할 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectRoleUseCases {
        // Set collection paths for repositories
        projectRoleRepository.setCollection(
            CollectionPath.projectRoles(projectId.value)
        )

        // Do not set permissionRepository collection here; scope per role in use cases

        return ProjectRoleUseCases(
            // 역할 기본 CRUD
            createRoleUseCase = CreateRoleUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            
            createProjectRoleUseCase = CreateProjectRoleUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            
            updateProjectRoleUseCase = UpdateProjectRoleUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            
            deleteRoleUseCase = DeleteRoleUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository,
                authRepository = this.authRepository
            ),
            
            // 역할 조회
            getProjectRoleUseCase = GetProjectRoleUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            
            getProjectRolesUseCase = GetProjectRolesUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            
            getRoleDetailsUseCase = GetRoleDetailsUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            getUserRolesForProjectUseCase = GetUserRolesForProjectUseCaseImpl(
                authRepository = this.authRepository,
                memberRepository = this.memberRepository
            ),
            
            // 권한 관리
            getRolePermissionsUseCase = GetRolePermissionsUseCaseImpl(
                projectRoleRepository = this.projectRoleRepository
            ),
            setRolePermissionsUseCase = com.example.domain_usecase.usecase.project.role.SetRolePermissionsUseCaseImpl(
                permissionRepository = this.permissionRepository
            ),
            getUserPermissionsForProjectUseCase = GetUserPermissionsForProjectUseCaseImpl(
                authRepository = this.authRepository,
                memberRepository = this.memberRepository,
                projectRoleRepository = this.projectRoleRepository
            )
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
    val getUserRolesForProjectUseCase: GetUserRolesForProjectUseCase,

    // 권한 관리
    val getRolePermissionsUseCase: GetRolePermissionsUseCase,
    val setRolePermissionsUseCase: SetRolePermissionsUseCase,
    val getUserPermissionsForProjectUseCase: GetUserPermissionsForProjectUseCase,
)
