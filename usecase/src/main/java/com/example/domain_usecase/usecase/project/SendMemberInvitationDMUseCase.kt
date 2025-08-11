package com.example.domain_usecase.usecase.project

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.data.UserSession
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain.vo.user.UserName
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.DMChannelRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.base.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

/**
 * DM 채널을 통해 멤버 초대 메시지를 전송하는 UseCase
 *
 * 작업 흐름:
 * 1. 대상 사용자와의 DM 채널 확인/생성
 * 2. 프로젝트 정보 가져오기
 * 3. SYSTEM_MEMBER_INVITATION 타입의 특수 메시지 생성
 * 4. DM 채널에 메시지 전송
 */
class SendMemberInvitationDMUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) {

    /**
     * 대상 사용자에게 DM으로 멤버 초대 메시지를 전송합니다.
     *
     * @param targetUserId 초대할 사용자 ID
     * @param targetUserName 초대할 사용자 이름 (DM 채널 생성용)
     * @param projectId 초대할 프로젝트 ID
     * @return 초대 메시지 전송 결과를 나타내는 Flow
     */
    operator fun invoke(
        targetUserId: UserId,
        targetUserName: UserName,
        projectId: DocumentId
    ): Flow<CustomResult<DocumentId, Exception>> = flow {
        emit(CustomResult.Loading)

        try {
            // 1. 현재 사용자 세션 확인
            val currentUserSession = authRepository.getCurrentUserSession()
            when (currentUserSession) {
                is CustomResult.Success -> {
                    val inviterSession = currentUserSession.data
                    val inviterName = inviterSession.displayName?.value ?: "사용자"

                    // 2. 프로젝트 정보 가져오기
                    val projectResult = projectRepository.findById(projectId)
                    when (projectResult) {
                        is CustomResult.Success -> {
                            val project = projectResult.data
                            val projectName = project.name.value

                            // 3. DM 채널 생성 또는 가져오기
                            val dmChannelResult =
                                dmChannelRepository.createDMChannel(targetUserName.value)
                            when (dmChannelResult) {
                                is CustomResult.Success -> {
                                    val resultData = dmChannelResult.data
                                    val dataObject = resultData["data"] as? Map<String, Any?>
                                    val channelId = dataObject?.get("channelId") as? String

                                    if (channelId != null) {
                                        // 4. 멤버 초대 메시지 생성
                                        val invitationMessage = createMemberInvitationMessage(
                                            senderId = inviterSession.userId,
                                            channelId = ChannelId(channelId),
                                            projectId = projectId.value,
                                            projectName = projectName,
                                            inviterName = inviterName,
                                            targetUserId = targetUserId.value
                                        )

                                        // 5. 메시지 저장 (Room DB에 저장되어 UI에 표시됨)
                                        val saveResult = messageRepository.save(invitationMessage)
                                        when (saveResult) {
                                            is CustomResult.Success -> {
                                                Log.d(
                                                    TAG,
                                                    "멤버 초대 DM 메시지 전송 성공: ${invitationMessage.id.value}"
                                                )
                                                emit(CustomResult.Success(invitationMessage.id))
                                            }

                                            is CustomResult.Failure -> {
                                                Log.e(TAG, "멤버 초대 메시지 저장 실패", saveResult.error)
                                                emit(CustomResult.Failure(Exception("초대 메시지 저장 실패: ${saveResult.error.message}")))
                                            }

                                            else -> {
                                                // Loading이나 다른 상태는 여기서 처리하지 않음
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "DM 채널 ID를 가져올 수 없음")
                                        emit(CustomResult.Failure(Exception("DM 채널 생성 실패")))
                                    }
                                }

                                is CustomResult.Failure -> {
                                    Log.e(TAG, "DM 채널 생성/조회 실패", dmChannelResult.error)
                                    emit(CustomResult.Failure(Exception("DM 채널 처리 실패: ${dmChannelResult.error.message}")))
                                }

                                else -> {
                                    // Loading 상태는 계속 대기
                                }
                            }
                        }

                        is CustomResult.Failure -> {
                            Log.e(TAG, "프로젝트 정보 조회 실패", projectResult.error)
                            emit(CustomResult.Failure(Exception("프로젝트 정보 조회 실패: ${projectResult.error.message}")))
                        }

                        else -> {
                            // Loading 상태는 계속 대기
                        }
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "사용자 인증 실패", currentUserSession.error)
                    emit(CustomResult.Failure(Exception("사용자 인증 실패: ${currentUserSession.error.message}")))
                }

                else -> {
                    // Loading 상태는 계속 대기
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "멤버 초대 DM 전송 중 예외 발생", e)
            emit(CustomResult.Failure(Exception("초대 메시지 전송 실패: ${e.message}", e)))
        }
    }

    /**
     * 멤버 초대 시스템 메시지를 생성합니다.
     */
    private fun createMemberInvitationMessage(
        senderId: UserId,
        channelId: ChannelId,
        projectId: String,
        projectName: String,
        inviterName: String,
        targetUserId: String
    ): Message {
        val messageId = DocumentId.generate()
        val currentTime = Instant.now()

        // SYSTEM_MEMBER_INVITATION 타입의 페이로드 생성
        val payload = MessagePayload.forMemberInvitation(
            projectId = projectId,
            projectName = projectName,
            inviterName = inviterName,
            targetUserId = targetUserId,
            actionText = "참여하기"
        )

        return Message.create(
            id = messageId,
            senderId = senderId,
            messageType = MessageType.SYSTEM_MEMBER_INVITATION,
            payload = payload,
            replyToMessageId = null,
            mentions = emptyList(),
            channelId = channelId
        )
    }

    companion object {
        private const val TAG = "SendMemberInvitationDMUseCase"
    }
}