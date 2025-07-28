package com.example.domain.provider

import com.example.domain.repository.local.ProjectPermissionLocalRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 권한 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 프로젝트 권한 확인, 설정, 검증 등의 기능을 담당합니다.
 * Role과는 별개의 Collection으로 관리되는 Permission 엔티티를 다룹니다.
 */
@Singleton
class ProjectPermissionUseCaseProvider @Inject constructor(
    private val projectPermissionLocalRepository: ProjectPermissionLocalRepository
) {

    /**
     * 프로젝트 권한 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 권한 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): ProjectPermissionLocalBasicUseCases {
        return ProjectPermissionLocalBasicUseCases(
            // TODO: 향후 permission basic local use cases 추가
            // - CheckUserPermissionLocalUseCase
            // - GetProjectPermissionsLocalUseCase
            // - ValidatePermissionLocalUseCase
            projectPermissionLocalRepository = projectPermissionLocalRepository
        )
    }

    /**
     * 프로젝트 권한 고급 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 권한 고급 관리 UseCase 그룹
     */
    fun createAdvancedUseCases(): ProjectPermissionLocalAdvancedUseCases {
        return ProjectPermissionLocalAdvancedUseCases(
            // TODO: 향후 permission advanced local use cases 추가
            // - UpdateUserPermissionsLocalUseCase
            // - BatchPermissionUpdateLocalUseCase
            // - PermissionAuditLocalUseCase
            projectPermissionLocalRepository = projectPermissionLocalRepository
        )
    }

    /**
     * 프로젝트 권한 검증 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 권한 검증 UseCase 그룹
     */
    fun createValidationUseCases(): ProjectPermissionLocalValidationUseCases {
        return ProjectPermissionLocalValidationUseCases(
            // TODO: 향후 permission validation local use cases 추가
            // - ValidateOperationPermissionLocalUseCase
            // - CheckResourceAccessLocalUseCase
            // - PermissionConflictCheckLocalUseCase
            projectPermissionLocalRepository = projectPermissionLocalRepository
        )
    }
}

/**
 * 프로젝트 권한 기본 관리 Local UseCase 그룹
 */
data class ProjectPermissionLocalBasicUseCases(
    val projectPermissionLocalRepository: ProjectPermissionLocalRepository
)

/**
 * 프로젝트 권한 고급 관리 Local UseCase 그룹
 */
data class ProjectPermissionLocalAdvancedUseCases(
    val projectPermissionLocalRepository: ProjectPermissionLocalRepository
)

/**
 * 프로젝트 권한 검증 Local UseCase 그룹
 */
data class ProjectPermissionLocalValidationUseCases(
    val projectPermissionLocalRepository: ProjectPermissionLocalRepository
) 