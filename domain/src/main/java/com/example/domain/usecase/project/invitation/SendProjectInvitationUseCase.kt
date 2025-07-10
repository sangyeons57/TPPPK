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
 * DDD 패턴에 따라 ProjectInvitation 도메인 모델을 생성하고 저장합니다.
 */
interface SendProjectInvitationUseCase {
    /**
     * 프로젝트 초대를 보냅니다.
     * 
     * @param projectId 프로젝트 ID
     * @param inviterId 초대를 보내는 사용자 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 생성된 초대 객체
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        inviterId: UserId,
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
     * @param inviterId 초대를 보내는 사용자 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 생성된 초대 객체
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        inviterId: UserId,
        expiresInHours: Long
    ): CustomResult<ProjectInvitation, Exception> {
        // DDD 패턴: 도메인 모델 생성
        val projectInvitation = ProjectInvitation.createNew(
            inviterId = inviterId,
            projectId = projectId,
            expiresInHours = expiresInHours
        )

        // 도메인 모델 저장
        return when (val saveResult = projectInvitationRepository.save(projectInvitation)) {
            is CustomResult.Success -> {
                CustomResult.Success(projectInvitation)
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(saveResult.error)
            }
            else -> saveResult as CustomResult<ProjectInvitation, Exception>
        }
    }
}