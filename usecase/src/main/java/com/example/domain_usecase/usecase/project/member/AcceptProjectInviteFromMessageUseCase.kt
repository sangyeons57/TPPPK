package com.example.domain_usecase.usecase.project.member

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.ProjectInvitationRepository
import com.example.domain_usecase.usecase.project.invitation.AcceptProjectInvitationUseCase
import javax.inject.Inject

/**
 * 특수 메시지에서 프로젝트 초대 참여 처리 UseCase
 * 
 * 프로젝트 초대 메시지의 "참여하기" 버튼 클릭 시 실행되며,
 * 초대 유효성 검증 후 프로젝트 멤버로 추가합니다.
 */
interface AcceptProjectInviteFromMessageUseCase {
    /**
     * 초대 ID를 통해 프로젝트 참여 처리
     * 
     * @param invitationId 초대 ID
     * @return 참여 처리 결과
     */
    suspend operator fun invoke(
        invitationId: String
    ): CustomResult<Unit, Exception>
}

/**
 * 프로젝트 초대 참여 처리 UseCase 구현체
 */
class AcceptProjectInviteFromMessageUseCaseImpl @Inject constructor(
    private val projectInvitationRepository: ProjectInvitationRepository,
    private val acceptProjectInvitationUseCase: AcceptProjectInvitationUseCase
) : AcceptProjectInviteFromMessageUseCase {

    private val TAG = "AcceptProjectInviteFromMessageUseCase"

    override suspend operator fun invoke(
        invitationId: String
    ): CustomResult<Unit, Exception> {
        
        Log.d(TAG, "프로젝트 초대 참여 처리 시작: invitationId=$invitationId")

        try {
            if (invitationId.isBlank()) {
                return CustomResult.Failure(Exception("초대 ID가 유효하지 않습니다."))
            }

            // 1. 초대 정보 조회
            val invitationResult = projectInvitationRepository.findById(DocumentId(invitationId))
            val invitation = when (invitationResult) {
                is CustomResult.Success -> invitationResult.data
                is CustomResult.Failure -> {
                    Log.e(TAG, "초대 정보를 찾을 수 없음: $invitationId")
                    return CustomResult.Failure(Exception("초대 정보를 찾을 수 없습니다."))
                }
                else -> {
                    return CustomResult.Failure(Exception("초대 정보 조회 중 오류가 발생했습니다."))
                }
            }

            // 2. 초대 유효성 검증 (만료, 이미 사용됨 등)
            // TODO: Add expiry and usage check methods to ProjectInvitation domain model
            // if (invitation.isExpired()) {
            //     Log.e(TAG, "만료된 초대: $invitationId")
            //     return CustomResult.Failure(Exception("만료된 초대입니다."))
            // }

            // if (invitation.isAlreadyUsed()) {
            //     Log.e(TAG, "이미 사용된 초대: $invitationId")
            //     return CustomResult.Failure(Exception("이미 사용된 초대입니다."))
            // }

            // 3. 기존 AcceptProjectInvitationUseCase 활용하여 참여 처리
            Log.d(TAG, "프로젝트 참여 처리 중: projectId=${invitation.projectId.value}")
            val acceptResult = acceptProjectInvitationUseCase(
                invitationId = DocumentId(invitation.id.value)
            )

            return when (acceptResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "프로젝트 참여 완료: projectId=${invitation.projectId.value}")
                    CustomResult.Success(Unit)
                }
                is CustomResult.Failure -> {
                    Log.e(TAG, "프로젝트 참여 실패", acceptResult.error)
                    CustomResult.Failure(acceptResult.error)
                }
                else -> {
                    CustomResult.Failure(Exception("참여 처리 중 알 수 없는 오류가 발생했습니다."))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "프로젝트 초대 참여 처리 중 예외 발생", e)
            return CustomResult.Failure(e)
        }
    }
}