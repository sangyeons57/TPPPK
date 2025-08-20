package com.example.domain_usecase.usecase.project.member

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_repository.base.UserRepository
import com.example.websocket.usecase.WebSocketUseCaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 프로젝트 초대 메시지를 DM으로 전송하는 UseCase
 *
 * 책임:
 * - 순수하게 메시지 생성 및 전송만 담당
 * - 채널 생성, 사용자 존재 확인 등은 외부에서 처리됨
 */
interface SendProjectInviteMessageUseCase {
    /**
     * 지정된 DM 채널에 프로젝트 초대 메시지 전송
     *
     * @param channelId DM 채널 ID
     * @param projectId 프로젝트 ID
     * @param targetUserId 초대 대상 사용자 ID
     * @return 전송 결과
     */
    operator fun invoke(
        channelId: ChannelId,
        projectId: DocumentId,
        targetUserId: UserId
    ): Flow<CustomResult<Unit, Exception>>
}

/**
 * 프로젝트 초대 메시지 전송 UseCase 구현체
 */
class SendProjectInviteMessageUseCaseImpl @Inject constructor(
    private val messageRepository: MessageRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
) : SendProjectInviteMessageUseCase {

    private val TAG = "SendProjectInviteMessageUseCase"

    override operator fun invoke(
        channelId: ChannelId,
        projectId: DocumentId,
        targetUserId: UserId
    ): Flow<CustomResult<Unit, Exception>> = flow {

        Log.d(TAG, "🚀 UseCase 호출됨 - 시작")
        emit(CustomResult.Loading)
        Log.d(TAG, "🚀 Loading 상태 emit 완료")

        try {
            Log.d(
                TAG,
                "프로젝트 초대 메시지 전송 시작 - channelId: ${channelId.value}, projectId: ${projectId.value}, targetUserId: ${targetUserId.value}"
            )

            // 1. 현재 사용자 정보 가져오기
            Log.d(TAG, "🔍 1단계: 현재 사용자 정보 조회 중...")
            val currentUserResult = authRepository.getCurrentUserSession()
            if (currentUserResult !is CustomResult.Success) {
                Log.e(TAG, "❌ 사용자 인증 실패")
                emit(CustomResult.Failure(Exception("사용자 인증이 필요합니다.")))
                return@flow
            }
            val currentUser = currentUserResult.data
            val inviterId = currentUser.userId
            Log.d(TAG, "✅ 1단계 완료: 사용자 정보 조회 성공 - inviterId: ${inviterId.value}")

            // 1-1. 실제 User 엔티티에서 사용자 이름 가져오기
            Log.d(TAG, "🔍 1-1단계: User 엔티티에서 사용자 이름 조회 중...")
            val userResult = userRepository.findById(DocumentId(inviterId.value))
            val inviterName = when (userResult) {
                is CustomResult.Success -> {
                    val actualUser = userResult.data
                    val userName = actualUser.name.value
                    Log.d(TAG, "✅ 1-1단계 완료: User 엔티티에서 사용자 이름 조회 성공 - inviterName: $userName")
                    userName
                }

                else -> {
                    // User 엔티티에서 가져오지 못한 경우 UserSession의 displayName 사용
                    val fallbackName =
                        currentUser.displayName?.value ?: currentUser.email?.value ?: "알 수 없음"
                    Log.w(TAG, "⚠️ 1-1단계 경고: User 엔티티 조회 실패, fallback 이름 사용: $fallbackName")
                    fallbackName
                }
            }
            
            // 2. 프로젝트 정보 가져오기
            Log.d(TAG, "🔍 2단계: 프로젝트 정보 조회 중...")
            val projectResult = projectRepository.findById(projectId)
            val project = when (projectResult) {
                is CustomResult.Success -> {
                    Log.d(
                        TAG,
                        "✅ 2단계 완료: 프로젝트 정보 조회 성공 - projectName: ${projectResult.data.name.value}"
                    )
                    projectResult.data
                }
                else -> {
                    Log.e(TAG, "❌ 프로젝트 정보 조회 실패")
                    emit(CustomResult.Failure(Exception("프로젝트 정보를 찾을 수 없습니다.")))
                    return@flow
                }
            }

            // 3. 프로젝트 초대 메시지 생성
            Log.d(TAG, "🔍 3단계: 프로젝트 초대 메시지 생성 중...")
            val messageId = DocumentId.generate()
            val payload = MessagePayload.forProjectInvite(
                projectId = project.id.value,
                projectName = project.name.value,
                inviterName = inviterName,
            )
            Log.d(TAG, "✅ 3단계 완료: 메시지 생성 완료 - messageId: ${messageId.value}")

            // 4. 메시지를 Repository에 저장 (WebSocket 전송은 MessageRepository 내부에서 처리)
            Log.d(TAG, "🔍 4단계: 메시지 전송 중...")
            val message = Message.create(
                id = messageId,
                senderId = inviterId,
                messageType = MessageType.PROJECT_INVITE,
                payload = payload,
                replyToMessageId = null,
                mentions = emptyList(),
                channelId = channelId
            )

            // 1) Local save (also enqueues OutBox inside repository transaction)
            when (val saveResult = messageRepository.sendMessage(message)) {
                is CustomResult.Failure -> {
                    emit(CustomResult.Failure(saveResult.error))
                }
                is CustomResult.Success -> {
                    val messageId = saveResult.data
                    // 2) WebSocket send (best-effort; return success on local save)
                    try {
                        // WebSocket room uses leaf channel id
                        val roomId = channelId.last()
                        val roomUseCases = webSocketUseCaseProvider.createForRoom(roomId)
                        // 도메인 메시지 기반 전송 API 사용
                        val wsResult = roomUseCases.sendMessageUseCase(
                            message = message
                        )
                        if (!wsResult.isSuccess) {
                            Log.e(
                                "SendProjectInviteMessageUseCase",
                                "WebSocket send failed for message: ${messageId.value}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "SendProjectInviteMessageUseCase",
                            "Exception during WebSocket send: ${messageId.value}",
                            e
                        )
                    }

                    CustomResult.Success(messageId)
                }
                else -> {
                    // Should not reach here, keep safe fallback
                    CustomResult.Failure(IllegalStateException("Unknown save result"))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 UseCase 전체 예외 발생", e)
            emit(CustomResult.Failure(e))
        }
    }
}
