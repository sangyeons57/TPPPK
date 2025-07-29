package com.example.domain.provider

import com.example.domain.repository.local.LocalProjectInvitationRepository
import com.example.domain.usecase.local.project.invitation.SendProjectInvitationLocalUseCase
import com.example.domain.usecase.local.project.invitation.SendProjectInvitationLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 초대 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 프로젝트 초대장 생성, 관리, 수락/거절 등의 기능을 담당합니다.
 */
@Singleton
class ProjectInvitationUseCaseProvider @Inject constructor(
    private val projectInvitationLocalRepository: LocalProjectInvitationRepository
) {

    /**
     * 프로젝트 초대 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 초대 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): ProjectInvitationLocalBasicUseCases {
        return ProjectInvitationLocalBasicUseCases(
            // 초대장 생성
            sendProjectInvitationLocalUseCase = SendProjectInvitationLocalUseCaseImpl(
                projectInvitationLocalRepository = projectInvitationLocalRepository
            ),
            
            // TODO: 향후 invitation basic local use cases 추가
            // - GetProjectInvitationsLocalUseCase
            // - GetInvitationDetailsLocalUseCase
            // - DeleteInvitationLocalUseCase
            projectInvitationLocalRepository = projectInvitationLocalRepository
        )
    }

    /**
     * 프로젝트 초대 응답 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 초대 응답 관리 UseCase 그룹
     */
    fun createResponseUseCases(): ProjectInvitationLocalResponseUseCases {
        return ProjectInvitationLocalResponseUseCases(
            // TODO: 향후 invitation response local use cases 추가
            // - AcceptInvitationLocalUseCase
            // - RejectInvitationLocalUseCase
            // - ValidateInvitationCodeLocalUseCase
            projectInvitationLocalRepository = projectInvitationLocalRepository
        )
    }

    /**
     * 프로젝트 초대 상태 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 초대 상태 관리 UseCase 그룹
     */
    fun createStatusUseCases(): ProjectInvitationLocalStatusUseCases {
        return ProjectInvitationLocalStatusUseCases(
            // TODO: 향후 invitation status local use cases 추가
            // - UpdateInvitationStatusLocalUseCase
            // - ExpireInvitationLocalUseCase
            // - ResendInvitationLocalUseCase
            // - GetPendingInvitationsLocalUseCase
            projectInvitationLocalRepository = projectInvitationLocalRepository
        )
    }
}

/**
 * 프로젝트 초대 기본 관리 Local UseCase 그룹
 */
data class ProjectInvitationLocalBasicUseCases(
    // 초대장 생성
    val sendProjectInvitationLocalUseCase: SendProjectInvitationLocalUseCase,

    val projectInvitationLocalRepository: LocalProjectInvitationRepository
)

/**
 * 프로젝트 초대 응답 관리 Local UseCase 그룹
 */
data class ProjectInvitationLocalResponseUseCases(
    val projectInvitationLocalRepository: LocalProjectInvitationRepository
)

/**
 * 프로젝트 초대 상태 관리 Local UseCase 그룹
 */
data class ProjectInvitationLocalStatusUseCases(
    val projectInvitationLocalRepository: LocalProjectInvitationRepository
) 