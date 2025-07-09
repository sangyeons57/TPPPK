package com.example.domain.usecase.project.invitation

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.base.ProjectInvitationRepository
import javax.inject.Inject

/**
 * 프로젝트 초대 보내기 UseCase
 * 
 * 사용자가 다른 사용자를 프로젝트에 초대할 때 사용합니다.
 */
interface SendProjectInvitationUseCase {
    /**
     * 프로젝트 초대를 보냅니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @param message 초대 메시지 (선택사항)
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 생성된 초대 객체
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        inviteeId: UserId,
        message: String? = null,
        expiresInHours: Long = 72L
    ): CustomResult<ProjectInvitation, Exception>
}

/**
 * 프로젝트 초대 보내기 UseCase 구현체
 */
class SendProjectInvitationUseCaseImpl @Inject constructor(
    private val projectInvitationRepository: ProjectInvitationRepository
) : SendProjectInvitationUseCase {

    /**
     * 프로젝트 초대를 보냅니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @param message 초대 메시지 (선택사항)
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 생성된 초대 객체
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        inviteeId: UserId,
        message: String?,
        expiresInHours: Long
    ): CustomResult<ProjectInvitation, Exception> {
        return projectInvitationRepository.sendInvitation(
            projectId = projectId,
            inviteeId = inviteeId,
            message = message,
            expiresInHours = expiresInHours
        )
    }
}