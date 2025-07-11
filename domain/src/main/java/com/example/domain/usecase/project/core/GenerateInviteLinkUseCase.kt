package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.ProjectInvitationRepository
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.base.ProjectInvitation
import javax.inject.Inject

/**
 * 프로젝트 초대 링크를 생성하는 UseCase
 * DDD 패턴에 따라 ProjectInvitation 도메인 모델을 생성하고 저장합니다.
 * 
 * @property projectInvitationRepository 프로젝트 초대 관련 기능을 제공하는 Repository
 */
class GenerateInviteLinkUseCase @Inject constructor(
    private val projectInvitationRepository: ProjectInvitationRepository
) {
    /**
     * 프로젝트 초대를 생성합니다.
     *
     * @param inviterId 초대를 생성하는 사용자 ID
     * @param projectId 프로젝트 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 24시간)
     * @return 성공 시 생성된 초대의 DocumentId, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(
        inviterId: UserId,
        projectId: DocumentId,
        expiresInHours: Int = 24
    ): CustomResult<DocumentId, Exception> {
        // 입력 값 검증
        if (expiresInHours <= 0) {
            return CustomResult.Failure(Exception("만료 시간은 0보다 커야 합니다."))
        }

        // DDD 패턴: 도메인 모델 생성
        val projectInvitation = ProjectInvitation.createNew(
            inviterId = inviterId,
            projectId = projectId,
            expiresInHours = expiresInHours.toLong()
        )

        // 도메인 모델 저장
        return projectInvitationRepository.save(projectInvitation)
    }
}

