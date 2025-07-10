package com.example.domain.usecase.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectInvitationRepository
import javax.inject.Inject

/**
 * 프로젝트 초대 거절 UseCase
 * 
 * 링크 기반 초대 시스템에서는 초대를 "거절"한다는 것은 초대 링크를 취소(REVOKED)하는 것입니다.
 * DDD 패턴에 따라 도메인 모델을 조회하고 상태를 변경한 후 저장합니다.
 */
interface RejectProjectInvitationUseCase {
    /**
     * 프로젝트 초대를 거절합니다 (초대 링크를 취소합니다).
     * 
     * @param invitationId 초대 ID
     * @return 취소된 초대 객체
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
     * 프로젝트 초대를 거절합니다 (초대 링크를 취소합니다).
     * 
     * @param invitationId 초대 ID
     * @return 취소된 초대 객체
     */
    override suspend operator fun invoke(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        // 1. 초대 정보 조회
        return when (val findResult = projectInvitationRepository.findById(invitationId)) {
            is CustomResult.Success -> {
                val invitation = findResult.data as ProjectInvitation
                
                // 2. 초대 링크를 취소 상태로 변경
                invitation.revoke()
                
                // 3. 변경된 초대 정보 저장
                when (val saveResult = projectInvitationRepository.save(invitation)) {
                    is CustomResult.Success -> {
                        CustomResult.Success(invitation)
                    }
                    is CustomResult.Failure -> {
                        CustomResult.Failure(saveResult.error)
                    }
                    else -> saveResult as CustomResult<ProjectInvitation, Exception>
                }
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(findResult.error)
            }
            else -> findResult as CustomResult<ProjectInvitation, Exception>
        }
    }
}