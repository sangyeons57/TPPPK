package com.example.domain.provider

import com.example.domain.repository.local.ProjectRoleLocalRepository
import com.example.domain.usecase.local.project.role.CreateProjectRoleLocalUseCase
import com.example.domain.usecase.local.project.role.CreateProjectRoleLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.CreateRoleLocalUseCase
import com.example.domain.usecase.local.project.role.CreateRoleLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.DeleteRoleLocalUseCase
import com.example.domain.usecase.local.project.role.DeleteRoleLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.GetProjectRoleLocalUseCase
import com.example.domain.usecase.local.project.role.GetProjectRoleLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.GetProjectRolesLocalUseCase
import com.example.domain.usecase.local.project.role.GetProjectRolesLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.GetRoleDetailsLocalUseCase
import com.example.domain.usecase.local.project.role.GetRoleDetailsLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.GetRolePermissionsLocalUseCase
import com.example.domain.usecase.local.project.role.GetRolePermissionsLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.UpdateMemberRolesLocalUseCase
import com.example.domain.usecase.local.project.role.UpdateMemberRolesLocalUseCaseImpl
import com.example.domain.usecase.local.project.role.UpdateProjectRoleLocalUseCase
import com.example.domain.usecase.local.project.role.UpdateProjectRoleLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 역할 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 프로젝트 역할 생성, 수정, 삭제, 권한 관리 등의 기능을 담당합니다.
 */
@Singleton
class ProjectRoleUseCaseProvider @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) {

    /**
     * 프로젝트 역할 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 역할 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): ProjectRoleLocalBasicUseCases {
        return ProjectRoleLocalBasicUseCases(
            // 역할 생성
            createProjectRoleLocalUseCase = CreateProjectRoleLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            createRoleLocalUseCase = CreateRoleLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            // 역할 조회
            getProjectRolesLocalUseCase = GetProjectRolesLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            getProjectRoleLocalUseCase = GetProjectRoleLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            getRoleDetailsLocalUseCase = GetRoleDetailsLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            // 역할 삭제
            deleteRoleLocalUseCase = DeleteRoleLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            projectRoleLocalRepository = projectRoleLocalRepository
        )
    }

    /**
     * 프로젝트 역할 고급 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 역할 고급 관리 UseCase 그룹
     */
    fun createAdvancedUseCases(): ProjectRoleLocalAdvancedUseCases {
        return ProjectRoleLocalAdvancedUseCases(
            // 역할 수정
            updateProjectRoleLocalUseCase = UpdateProjectRoleLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            // 권한 관리
            getRolePermissionsLocalUseCase = GetRolePermissionsLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            // 멤버 역할 관리
            updateMemberRolesLocalUseCase = UpdateMemberRolesLocalUseCaseImpl(
                projectRoleLocalRepository = projectRoleLocalRepository
            ),
            
            projectRoleLocalRepository = projectRoleLocalRepository
        )
    }
}

/**
 * 프로젝트 역할 기본 관리 Local UseCase 그룹
 */
data class ProjectRoleLocalBasicUseCases(
    // 역할 생성
    val createProjectRoleLocalUseCase: CreateProjectRoleLocalUseCase,
    val createRoleLocalUseCase: CreateRoleLocalUseCase,
    
    // 역할 조회
    val getProjectRolesLocalUseCase: GetProjectRolesLocalUseCase,
    val getProjectRoleLocalUseCase: GetProjectRoleLocalUseCase,
    val getRoleDetailsLocalUseCase: GetRoleDetailsLocalUseCase,
    
    // 역할 삭제
    val deleteRoleLocalUseCase: DeleteRoleLocalUseCase,
    
    val projectRoleLocalRepository: ProjectRoleLocalRepository
)

/**
 * 프로젝트 역할 고급 관리 Local UseCase 그룹
 */
data class ProjectRoleLocalAdvancedUseCases(
    // 역할 수정
    val updateProjectRoleLocalUseCase: UpdateProjectRoleLocalUseCase,
    
    // 권한 관리
    val getRolePermissionsLocalUseCase: GetRolePermissionsLocalUseCase,
    
    // 멤버 역할 관리
    val updateMemberRolesLocalUseCase: UpdateMemberRolesLocalUseCase,
    
    val projectRoleLocalRepository: ProjectRoleLocalRepository
) 