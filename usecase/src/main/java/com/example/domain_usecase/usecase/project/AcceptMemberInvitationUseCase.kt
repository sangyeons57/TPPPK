package com.example.domain_usecase.usecase.project

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * DM 멤버 초대 수락 UseCase
 *
 * DM으로 받은 멤버 초대 메시지의 버튼을 눌렀을 때 실행되며,
 * 사용자를 해당 프로젝트의 멤버로 추가합니다.
 */
class AcceptMemberInvitationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider
) {

    /**
     * 멤버 초대를 수락하여 프로젝트에 참여합니다.
     *
     * @param projectId 참여할 프로젝트 ID
     * @param targetUserId 참여할 사용자 ID (일반적으로 현재 사용자)
     * @return 참여 결과를 나타내는 Flow
     */
    operator fun invoke(
        projectId: String,
        targetUserId: String
    ): Flow<CustomResult<Unit, Exception>> = flow {
        emit(CustomResult.Loading)

        try {
            Log.d(TAG, "멤버 초대 수락 시작: projectId=$projectId, targetUserId=$targetUserId")

            // 1. 현재 사용자 확인
            val currentUserSession = authRepository.getCurrentUserSession()
            when (currentUserSession) {
                is CustomResult.Success -> {
                    val currentUserId = currentUserSession.data.userId

                    // 2. 대상 사용자가 현재 사용자인지 확인 (보안 검증)
                    if (currentUserId.value != targetUserId) {
                        Log.e(
                            TAG,
                            "타인의 초대를 수락할 수 없습니다: currentUserId=${currentUserId.value}, targetUserId=$targetUserId"
                        )
                        emit(CustomResult.Failure(Exception("본인의 초대만 수락할 수 있습니다.")))
                        return@flow
                    }

                    // 3. 프로젝트 존재 확인
                    val projectResult = projectRepository.findById(DocumentId(projectId))
                    when (projectResult) {
                        is CustomResult.Success -> {
                            val project = projectResult.data
                            Log.d(TAG, "프로젝트 확인 완료: ${project.name.value}")

                            // 4. 프로젝트 멤버 추가
                            val projectMemberUseCases =
                                projectMemberUseCaseProvider.createForProject(DocumentId(projectId))

                            val addMemberResult = projectMemberUseCases.addProjectMemberUseCase(
                                userId = UserId(targetUserId),
                                initialRoleIds = emptyList() // 기본 역할로 추가
                            )

                            when (addMemberResult) {
                                is CustomResult.Success -> {
                                    Log.d(
                                        TAG,
                                        "프로젝트 멤버 추가 성공: projectId=$projectId, userId=$targetUserId"
                                    )
                                    emit(CustomResult.Success(Unit))
                                }

                                is CustomResult.Failure -> {
                                    Log.e(TAG, "프로젝트 멤버 추가 실패", addMemberResult.error)
                                    // 이미 멤버인 경우도 성공으로 처리
                                    if (addMemberResult.error.message?.contains("이미 멤버") == true) {
                                        Log.d(TAG, "이미 프로젝트 멤버임, 성공으로 처리")
                                        emit(CustomResult.Success(Unit))
                                    } else {
                                        emit(CustomResult.Failure(Exception("프로젝트 참여 실패: ${addMemberResult.error.message}")))
                                    }
                                }

                                else -> {
                                    // Loading 상태는 대기
                                }
                            }
                        }

                        is CustomResult.Failure -> {
                            Log.e(TAG, "프로젝트 조회 실패", projectResult.error)

                            // 권한 오류와 기타 오류 구분하여 처리
                            val errorMessage = when {
                                projectResult.error.message?.contains("PERMISSION_DENIED") == true -> {
                                    "프로젝트 접근 권한이 없습니다. 초대가 유효한지 확인해 주세요."
                                }

                                projectResult.error.message?.contains("UNAUTHENTICATED") == true -> {
                                    "로그인이 필요합니다. 다시 로그인해 주세요."
                                }

                                else -> {
                                    "프로젝트를 찾을 수 없습니다. 네트워크 상태를 확인해 주세요."
                                }
                            }

                            emit(CustomResult.Failure(Exception(errorMessage)))
                        }

                        else -> {
                            // Loading 상태는 대기
                        }
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "사용자 인증 실패", currentUserSession.error)
                    emit(CustomResult.Failure(Exception("사용자 인증 실패: ${currentUserSession.error.message}")))
                }

                else -> {
                    // Loading 상태는 대기
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "멤버 초대 수락 중 예외 발생", e)
            emit(CustomResult.Failure(Exception("초대 수락 실패: ${e.message}", e)))
        }
    }

    companion object {
        private const val TAG = "AcceptMemberInvitationUseCase"
    }
}