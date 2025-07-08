package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.model.vo.DocumentId
import javax.inject.Inject

/**
 * 프로젝트 초대 링크를 생성하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class GenerateInviteLinkUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 프로젝트 초대 링크를 생성합니다.
     *
     * @param projectId 프로젝트 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 24시간)
     * @param maxUses 최대 사용 횟수 (nullable)
     * @return 성공 시 초대 링크 정보, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        expiresInHours: Int = 24,
        maxUses: Int? = null
    ): CustomResult<InviteLinkData, Exception> {
        if (expiresInHours <= 0) {
            return CustomResult.Failure(Exception("만료 시간은 0보다 커야 합니다."))
        }

        maxUses?.let { uses ->
            if (uses <= 0) {
                return CustomResult.Failure(Exception("최대 사용 횟수는 0보다 커야 합니다."))
            }
        }

        return when (val result = projectRepository.generateInviteLink(projectId, expiresInHours, maxUses)) {
            is CustomResult.Success -> {
                val inviteCode = result.data["inviteCode"] as? String
                val inviteLink = result.data["inviteLink"] as? String
                val expiresAt = result.data["expiresAt"] as? Any
                val maxUsesResult = result.data["maxUses"] as? Int

                if (inviteCode != null && inviteLink != null) {
                    CustomResult.Success(
                        InviteLinkData(
                            inviteCode = inviteCode,
                            inviteLink = inviteLink,
                            expiresAt = expiresAt,
                            maxUses = maxUsesResult
                        )
                    )
                } else {
                    CustomResult.Failure(Exception("초대 링크 생성 응답에서 필수 정보를 찾을 수 없습니다."))
                }
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(result.error)
            }
            is CustomResult.Loading -> {
                CustomResult.Loading
            }
            is CustomResult.Initial -> {
                CustomResult.Initial
            }
            is CustomResult.Progress -> {
                CustomResult.Loading
            }
        }
    }
}

/**
 * 초대 링크 데이터 클래스
 */
data class InviteLinkData(
    val inviteCode: String,
    val inviteLink: String,
    val expiresAt: Any?,
    val maxUses: Int?
)