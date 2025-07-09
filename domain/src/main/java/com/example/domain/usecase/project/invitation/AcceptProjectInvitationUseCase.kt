package com.example.domain.usecase.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectInvitationRepository
import javax.inject.Inject

/**
 * 프로젝트 초대 수락 UseCase
 * 
 * 사용자가 받은 프로젝트 초대를 수락할 때 사용합니다.
 */
interface AcceptProjectInvitationUseCase {
    /**
     * 프로젝트 초대를 수락합니다.
     * 
     * @param invitationId 초대 ID
     * @return 수락된 초대 객체
     */
    suspend operator fun invoke(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception>
}

/**
 * 프로젝트 초대 수락 UseCase 구현체
 */
class AcceptProjectInvitationUseCaseImpl @Inject constructor(
    private val projectInvitationRepository: ProjectInvitationRepository
) : AcceptProjectInvitationUseCase {

    /**
     * 프로젝트 초대를 수락합니다.
     * 
     * @param invitationId 초대 ID
     * @return 수락된 초대 객체
     */
    override suspend operator fun invoke(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        return projectInvitationRepository.acceptInvitation(invitationId)
    }
}