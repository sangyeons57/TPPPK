package com.example.domain.provider

import com.example.domain.repository.local.LocalProjectMemberRepository
import com.example.domain.usecase.local.project.member.AddProjectMemberLocalUseCase
import com.example.domain.usecase.local.project.member.AddProjectMemberLocalUseCaseImpl
import com.example.domain.usecase.local.project.member.GetProjectMemberLocalUseCase
import com.example.domain.usecase.local.project.member.GetProjectMemberLocalUseCaseImpl
import com.example.domain.usecase.local.project.member.ObserveProjectMembersLocalUseCase
import com.example.domain.usecase.local.project.member.ObserveProjectMembersLocalUseCaseImpl
import com.example.domain.usecase.local.project.member.RemoveProjectMemberLocalUseCase
import com.example.domain.usecase.local.project.member.RemoveProjectMemberLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 멤버 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 프로젝트 멤버 추가, 제거, 조회, 권한 관리 등의 기능을 담당합니다.
 */
@Singleton
class ProjectMemberUseCaseProvider @Inject constructor(
    private val projectMemberLocalRepository: LocalProjectMemberRepository
) {

    /**
     * 프로젝트 멤버 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 멤버 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): ProjectMemberLocalBasicUseCases {
        return ProjectMemberLocalBasicUseCases(
            // 멤버 추가/제거
            addProjectMemberLocalUseCase = AddProjectMemberLocalUseCaseImpl(
                projectMemberLocalRepository = projectMemberLocalRepository
            ),
            
            removeProjectMemberLocalUseCase = RemoveProjectMemberLocalUseCaseImpl(
                projectMemberLocalRepository = projectMemberLocalRepository
            ),
            
            // 멤버 조회
            getProjectMemberLocalUseCase = GetProjectMemberLocalUseCaseImpl(
                projectMemberLocalRepository = projectMemberLocalRepository
            ),
            
            observeProjectMembersLocalUseCase = ObserveProjectMembersLocalUseCaseImpl(
                projectMemberLocalRepository = projectMemberLocalRepository
            ),
            
            projectMemberLocalRepository = projectMemberLocalRepository
        )
    }

    /**
     * 프로젝트 멤버 고급 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 멤버 고급 관리 UseCase 그룹
     */
    fun createAdvancedUseCases(): ProjectMemberLocalAdvancedUseCases {
        return ProjectMemberLocalAdvancedUseCases(
            // TODO: 향후 member advanced local use cases 추가
            // - UpdateMemberRoleLocalUseCase
            // - TransferOwnershipLocalUseCase
            // - UpdateMemberStatusLocalUseCase
            // - GetMembersByRoleLocalUseCase
            projectMemberLocalRepository = projectMemberLocalRepository
        )
    }

    /**
     * 프로젝트 멤버 권한 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 멤버 권한 관리 UseCase 그룹
     */
    fun createPermissionUseCases(): ProjectMemberLocalPermissionUseCases {
        return ProjectMemberLocalPermissionUseCases(
            // TODO: 향후 member permission local use cases 추가
            // - CheckMemberPermissionLocalUseCase
            // - GrantMemberPermissionLocalUseCase
            // - RevokeMemberPermissionLocalUseCase
            // - GetMemberPermissionsLocalUseCase
            projectMemberLocalRepository = projectMemberLocalRepository
        )
    }
}

/**
 * 프로젝트 멤버 기본 관리 Local UseCase 그룹
 */
data class ProjectMemberLocalBasicUseCases(
    // 멤버 추가/제거
    val addProjectMemberLocalUseCase: AddProjectMemberLocalUseCase,
    val removeProjectMemberLocalUseCase: RemoveProjectMemberLocalUseCase,
    
    // 멤버 조회
    val getProjectMemberLocalUseCase: GetProjectMemberLocalUseCase,
    val observeProjectMembersLocalUseCase: ObserveProjectMembersLocalUseCase,

    val projectMemberLocalRepository: LocalProjectMemberRepository
)

/**
 * 프로젝트 멤버 고급 관리 Local UseCase 그룹
 */
data class ProjectMemberLocalAdvancedUseCases(
    val projectMemberLocalRepository: LocalProjectMemberRepository
)

/**
 * 프로젝트 멤버 권한 관리 Local UseCase 그룹
 */
data class ProjectMemberLocalPermissionUseCases(
    val projectMemberLocalRepository: LocalProjectMemberRepository
) 