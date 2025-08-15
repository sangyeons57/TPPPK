package com.example.domain_usecase.usecase.project

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.data.UserSession
import com.example.domain.vo.ChannelId
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain.vo.user.UserName
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.DMChannelRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_usecase.usecase.message.SendMessageUseCase
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
@Deprecated(
    message = "Orchestrate in ViewModel: use AddDmChannelUseCase/GetDmChannelUseCase + MessagePayload.forProjectInviteBasic + core/websocket SendMessageUseCase",
    replaceWith = ReplaceWith("Use AddDmChannelUseCase, GetDmChannelUseCase, MessagePayload.forProjectInviteBasic, and core/websocket SendMessageUseCase from ViewModel")
)
class SendMemberInvitationDMUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    // Domain-layer send (local save); WS send should be triggered at feature/app layer to avoid module cycle
    private val sendMessageUseCase: SendMessageUseCase
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

                            // 3. DM 채널 생성 또는 가져오기 (2단계 접근법)
                            var channelId: String? = null

                            // 3-1. DM 채널 생성 시도
                            val dmChannelResult =
                                dmChannelRepository.createDMChannel(targetUserName.value)
                            when (dmChannelResult) {
                                is CustomResult.Success -> {
                                    val resultData = dmChannelResult.data
                                    val dataObject = resultData["data"] as? Map<String, Any?>
                                    channelId = dataObject?.get("channelId") as? String
                                    
                                    if (channelId != null) {
                                        Log.d(TAG, "DM 채널 생성 성공: $channelId")
                                    }
                                }

                                is CustomResult.Failure -> {
                                    // 3-2. 생성 실패 시 기존 채널이 있는지 확인
                                    val errorMessage = dmChannelResult.error.message ?: ""
                                    if (errorMessage.contains(
                                            "already exists",
                                            ignoreCase = true
                                        ) ||
                                        errorMessage.contains(
                                            "dmChannel with exists",
                                            ignoreCase = true
                                        )
                                    ) {
                                        Log.d(TAG, "DM 채널이 이미 존재함. 기존 채널 조회 중...")

                                        // 기존 채널 조회 시도 (상대 사용자 ID 기반)
                                        when (val existing =
                                            dmChannelRepository.findByOtherUserId(targetUserId.value)) {
                                            is CustomResult.Success -> {
                                                channelId = existing.data.id.value
                                                Log.d(TAG, "기존 DM 채널 확인됨: $channelId")
                                            }

                                            is CustomResult.Failure -> {
                                                Log.e(TAG, "기존 DM 채널 조회 실패", existing.error)
                                            }

                                            else -> {
                                                // ignore Loading/Initial/Progress
                                            }
                                        }
                                    }

                                    if (channelId == null) {
                                        Log.e(TAG, "DM 채널 처리 실패: $errorMessage")
                                        emit(CustomResult.Failure(Exception("DM 채널 처리 실패: $errorMessage")))
                                        return@flow
                                    }
                                }

                                else -> {
                                    // Loading 상태는 계속 대기
                                    return@flow
                                }
                            }

                            // 3-3. 채널 ID가 확보된 경우에만 진행
                            if (channelId != null) {
                                // 4. 멤버 초대 메시지 페이로드 생성 (Project 전용 고정 포맷, 초대ID 없이)
                                val invitationPayload = MessagePayload.forProjectInviteBasic(
                                    projectId = projectId.value,
                                    projectName = projectName,
                                    inviterName = inviterName,
                                    targetUserId = targetUserId.value
                                )

                                // 5. Repository 컬렉션 설정 (DM 채널 메시지)
                                messageRepository.setCollection(
                                    CollectionPath.dmChannelMessages(
                                        channelId
                                    )
                                )

                                // 6. 도메인 SendMessageUseCase 사용 (로컬 저장) - WS 전송은 상위 레이어에서 호출
                                val sendResult = sendMessageUseCase(
                                    channelId = ChannelId(channelId),
                                    messageType = MessageType.SYSTEM_MEMBER_INVITATION,
                                    payload = invitationPayload
                                )

                                when (sendResult) {
                                    is CustomResult.Success -> {
                                        val messageId = sendResult.data.id
                                        Log.d(
                                            TAG,
                                            "멤버 초대 DM 메시지 전송 성공(로컬 저장): ${messageId.value}"
                                        )
                                        emit(CustomResult.Success(messageId))
                                    }

                                    is CustomResult.Failure -> {
                                        Log.e(TAG, "멤버 초대 메시지 전송 실패", sendResult.error)
                                        emit(CustomResult.Failure(Exception("초대 메시지 전송 실패: ${sendResult.error.message}")))
                                    }

                                    else -> {
                                        // Loading이나 다른 상태는 여기서 처리하지 않음
                                    }
                                }
                            } else {
                                Log.e(TAG, "DM 채널 ID를 최종적으로 확보할 수 없음")
                                emit(CustomResult.Failure(Exception("DM 채널 처리 실패: 채널 ID를 확보할 수 없습니다")))
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
     * 멤버 초대 시스템 메시지 페이로드를 생성합니다.
     * 통합 SendMessageUseCase가 자동으로 MessageType을 감지하고 현재 사용자를 설정합니다.
     */
    private fun createMemberInvitationPayload(
        projectId: String,
        projectName: String,
        inviterName: String,
        targetUserId: String
    ): MessagePayload {
        // SYSTEM_MEMBER_INVITATION 타입의 페이로드 생성 (JSON 문자열로 직접 구성)
        val jsonString = """
            {
                "${MessagePayload.KEY_CONTENT}": "$inviterName 님이 $projectName 프로젝트에 초대했습니다",
                "projectId": "$projectId",
                "projectName": "$projectName",
                "inviterName": "$inviterName",
                "targetUserId": "$targetUserId",
                "actionText": "참여하기"
            }
        """.trimIndent()

        return MessagePayload(jsonString)
    }

    companion object {
        private const val TAG = "SendMemberInvitationDMUseCase"
    }
}
