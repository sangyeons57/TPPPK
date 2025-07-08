package com.example.domain.usecase.project.core

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.ProjectRepository
import javax.inject.Inject

/**
 * 초대 코드를 검증하는 UseCase
 * 
 * @property projectRepository 프로젝트 관련 기능을 제공하는 Repository
 */
class ValidateInviteCodeUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /**
     * 초대 코드의 유효성을 검증합니다.
     *
     * @param inviteCode 검증할 초대 코드
     * @return 성공 시 초대 코드 검증 정보, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(inviteCode: String): CustomResult<InviteValidationData, Exception> {
        if (inviteCode.isBlank()) {
            return CustomResult.Failure(Exception("초대 코드가 비어있습니다."))
        }

        return when (val result = projectRepository.validateInviteCode(inviteCode)) {
            is CustomResult.Success -> {
                val valid = result.data["valid"] as? Boolean ?: false
                val projectId = result.data["projectId"] as? String
                val projectName = result.data["projectName"] as? String
                val projectImage = result.data["projectImage"] as? String
                val expiresAt = result.data["expiresAt"] as? Any
                val maxUses = result.data["maxUses"] as? Int
                val currentUses = result.data["currentUses"] as? Int
                val isAlreadyMember = result.data["isAlreadyMember"] as? Boolean ?: false
                val errorMessage = result.data["errorMessage"] as? String

                CustomResult.Success(
                    InviteValidationData(
                        valid = valid,
                        projectId = projectId,
                        projectName = projectName,
                        projectImage = projectImage,
                        expiresAt = expiresAt,
                        maxUses = maxUses,
                        currentUses = currentUses,
                        isAlreadyMember = isAlreadyMember,
                        errorMessage = errorMessage
                    )
                )
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
 * 초대 코드 검증 데이터 클래스
 */
data class InviteValidationData(
    val valid: Boolean,
    val projectId: String? = null,
    val projectName: String? = null,
    val projectImage: String? = null,
    val expiresAt: Any? = null,
    val maxUses: Int? = null,
    val currentUses: Int? = null,
    val isAlreadyMember: Boolean = false,
    val errorMessage: String? = null
)