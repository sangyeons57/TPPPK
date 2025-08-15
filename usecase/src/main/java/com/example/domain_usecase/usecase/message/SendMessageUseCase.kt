package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.MentionType
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MessageRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 통합 메시지 전송 - 현재 사용자 자동 조회
     *
     * @param channelId 메시지를 전송할 채널 ID
     * @param messageType 메시지 타입 (명시적 지정)
     * @param payload 메시지 내용 (MessagePayload)
     * @param replyToMessageId 답장 대상 메시지 ID (옵션)
     * @param mentions 멘션 목록 (옵션, payload에서 자동 추출 가능)
     * @return 생성된 메시지 또는 오류
     */
    suspend operator fun invoke(
        channelId: ChannelId,
        messageType: MessageType,
        payload: MessagePayload,
        replyToMessageId: DocumentId? = null,
        mentions: List<MentionInfo> = emptyList()
    ): CustomResult<Message, Exception> {
        return try {
            // 1. 현재 사용자 조회
            val currentUserSession = authRepository.getCurrentUserSession()
            val senderId = when (currentUserSession) {
                is CustomResult.Success -> UserId(currentUserSession.data.userId.value)
                is CustomResult.Failure -> return CustomResult.Failure(
                    Exception("Failed to get current user: ${currentUserSession.error.message}")
                )

                else -> return CustomResult.Failure(Exception("Unknown auth result type"))
            }

            // 2. 멘션/답장 정보는 Message 도메인 필드로만 관리 (Payload에서 추출하지 않음)
            val finalMentions = mentions
            val finalReplyToMessageId = replyToMessageId

            // 4. 메시지 생성 및 저장
            val messageId = DocumentId.generate()
            val message = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = messageType,
                payload = payload,
                replyToMessageId = finalReplyToMessageId,
                mentions = finalMentions,
                channelId = channelId
            )

            // 5. MessageRepositoryImpl에서 자동으로 OutBox 처리
            when (val result = messageRepository.save(message)) {
                is CustomResult.Success -> CustomResult.Success(message)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown result type"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 메시지 객체를 직접 저장
     * MessageRepositoryImpl에서 OutBox 패턴을 자동으로 처리
     */
    suspend operator fun invoke(message: Message): CustomResult<Message, Exception> {
        return try {
            when (val result = messageRepository.save(message)) {
                is CustomResult.Success -> CustomResult.Success(message)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown result type"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 레거시 호환성을 위한 senderId 파라미터가 있는 메서드
     * @deprecated Use invoke(channelId, payload) instead
     */
    @Deprecated(
        "Use invoke(channelId, messageType, payload) for automatic user detection",
        ReplaceWith("invoke(channelId, messageType, payload, replyToMessageId, mentions)")
    )
    suspend fun sendMessage(
        senderId: UserId,
        messageType: MessageType = MessageType.TEXT,
        payload: MessagePayload,
        replyToMessageId: DocumentId? = null,
        mentions: List<MentionInfo> = emptyList(),
        channelId: ChannelId
    ): CustomResult<Message, Exception> {
        return try {
            val messageId = DocumentId.generate()
            // messageType은 파라미터로 전달받음
            val message = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = messageType,
                payload = payload,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = channelId
            )

            when (val result = messageRepository.save(message)) {
                is CustomResult.Success -> CustomResult.Success(message)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown result type"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 메시지 동기화 상태 조회 (OutBox 기반)
     */
    suspend fun getMessageOutBoxStatus(messageId: DocumentId): CustomResult<OutBoxStatus, Exception> =
        messageRepository.getMessageOutBoxStatus(messageId)

    /**
     * 메시지 ACK 처리 (WebSocket ACK 수신 시)
     */
    suspend fun handleMessageAck(
        messageId: String
    ): CustomResult<Unit, Exception> {
        return try {
            messageRepository.handleMessageAck(messageId)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // No payload parsing; mentions/reply are provided via Message fields/parameters.
}
