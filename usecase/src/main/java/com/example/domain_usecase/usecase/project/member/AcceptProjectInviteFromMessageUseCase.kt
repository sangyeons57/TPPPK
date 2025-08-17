package com.example.domain_usecase.usecase.project.member

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.ProjectInvitationRepository
import com.example.domain_repository.base.ProjectRepository
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
    private val projectRepository: ProjectRepository,
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

            // 1) 초대 정보 조회 (읽기는 rules에서 허용됨)
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

            // 2) 서버(Cloud Functions)로 참여 처리 위임 (projectId 기반)
            Log.d(TAG, "서버 참여 처리 호출(projectId 기반): projectId=${invitation.projectId.value}")
            val joinResult = projectRepository.joinProject(invitation.projectId.toDocumentId())
            return when (joinResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "프로젝트 참여 완료(Functions): projectId=${invitation.projectId.value}")
                    CustomResult.Success(Unit)
                }
                is CustomResult.Failure -> {
                    Log.e(TAG, "프로젝트 참여 실패(Functions)", joinResult.error)
                    CustomResult.Failure(Exception(joinResult.error.message ?: "프로젝트 참여에 실패했습니다."))
                }
                is CustomResult.Initial -> CustomResult.Failure(Exception("초기 상태: 참여 처리 실패"))
                is CustomResult.Loading -> CustomResult.Failure(Exception("로딩 상태: 참여 처리 실패"))
                is CustomResult.Progress -> CustomResult.Failure(Exception("진행 상태: 참여 처리 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "프로젝트 초대 참여 처리 중 예외 발생", e)
            return CustomResult.Failure(e)
        }
    }
}
