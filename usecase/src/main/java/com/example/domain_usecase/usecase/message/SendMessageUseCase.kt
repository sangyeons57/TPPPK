package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import com.example.domain.model.vo.ChannelId
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
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
        content: MessageContent,
        replyToMessageId: DocumentId? = null,
        mentions: List<MentionInfo> = emptyList(),
        channelId: ChannelId
    ): CustomResult<Message, Exception> {
        return try {
            val messageId = DocumentId.generate()
            val message = Message.create(
                id = messageId,
                senderId = senderId,
                content = content,
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
     * 메시지 동기화 상태 업데이트
     */
    suspend fun updateMessageStatus(
        messageId: DocumentId,
        status: SyncStatus
    ): CustomResult<Unit, Exception> {
        return try {
            messageRepository.updateSyncStatus(messageId, status)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}