package com.example.domain.usecase.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectInvitationRepository
import javax.inject.Inject

/**
 * 프로젝트 초대 수락 UseCase
 * 
 * 링크 기반 초대 시스템에서는 초대 "수락"이란 실제로는 초대 링크를 통해 프로젝트에 참여하는 것입니다.
 * 이 UseCase는 초대 링크의 유효성을 확인하는 용도로 사용됩니다.
 */
interface AcceptProjectInvitationUseCase {
    /**
     * 프로젝트 초대 링크의 유효성을 확인합니다.
     * 
     * @param invitationId 초대 ID
     * @return 확인된 초대 객체
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
     * 프로젝트 초대 링크의 유효성을 확인합니다.
     * 
     * @param invitationId 초대 ID
     * @return 확인된 초대 객체
     */
    override suspend operator fun invoke(
        invitationId: DocumentId
    ): CustomResult<ProjectInvitation, Exception> {
        // 1. 초대 정보 조회
        return when (val findResult = projectInvitationRepository.findById(invitationId)) {
            is CustomResult.Success -> {
                val invitation = findResult.data as ProjectInvitation
                
                // 2. 초대가 활성 상태이고 사용 가능한지 확인
                if (invitation.canBeUsed()) {
                    CustomResult.Success(invitation)
                } else {
                    CustomResult.Failure(Exception("초대가 만료되었거나 사용할 수 없습니다."))
                }
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(findResult.error)
            }
            else -> findResult as CustomResult<ProjectInvitation, Exception>
        }
    }
}