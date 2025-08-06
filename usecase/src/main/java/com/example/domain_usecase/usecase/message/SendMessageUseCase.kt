package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MentionInfo
import com.example.domain.vo.message.MessagePayload
import com.example.domain_repository.base.MessageRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * 메시지 생성 및 저장
     * MessageRepositoryImpl에서 OutBox 패턴을 자동으로 처리
     */
    suspend operator fun invoke(
        senderId: UserId,
        payload: MessagePayload,
        replyToMessageId: DocumentId? = null,
        mentions: List<MentionInfo> = emptyList(),
        channelId: ChannelId
    ): CustomResult<Message, Exception> {
        return try {
            val messageId = DocumentId.generate()
            val message = Message.create(
                id = messageId,
                senderId = senderId,
                payload = payload,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = channelId
            )

            // MessageRepositoryImpl에서 자동으로 OutBox 처리
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
}