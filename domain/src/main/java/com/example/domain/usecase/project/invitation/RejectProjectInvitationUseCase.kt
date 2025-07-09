package com.example.domain.usecase.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectInvitationRepository
import javax.inject.Inject

/**
 * 프로젝트 초대 거절 UseCase
 * 
 * 사용자가 받은 프로젝트 초대를 거절할 때 사용합니다.
 */
interface RejectProjectInvitationUseCase {
    /**
     * 프로젝트 초대를 거절합니다.
     * 
     * @param invitationId 초대 ID
     * @return 거절된 초대 객체
     */
    suspend operator fun invoke(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception>
}

/**
 * 프로젝트 초대 거절 UseCase 구현체
 */
class RejectProjectInvitationUseCaseImpl @Inject constructor(
    private val projectInvitationRepository: ProjectInvitationRepository
) : RejectProjectInvitationUseCase {

    /**
     * 프로젝트 초대를 거절합니다.
     * 
     * @param invitationId 초대 ID
     * @return 거절된 초대 객체
     */
    override suspend operator fun invoke(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        return projectInvitationRepository.rejectInvitation(invitationId)
    }
}