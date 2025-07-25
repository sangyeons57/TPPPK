package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.repository.base.MessageRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    // 기존 메서드 (하위 호환성 유지)
    suspend operator fun invoke(
        senderId: UserId,
        content: MessageContent,
        replyToMessageId: DocumentId? = null,
        mentions: List<MentionInfo> = emptyList()
    ): CustomResult<Message, Exception> {
        return try {
            val messageId = DocumentId.generate()
            val message = Message.create(
                id = messageId,
                senderId = senderId,
                content = content,
                replyToMessageId = replyToMessageId,
                mentions = mentions
            )
            
            when (val saveResult = messageRepository.save(message)) {
                is CustomResult.Success -> CustomResult.Success(message)
                is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                is CustomResult.Initial -> CustomResult.Failure(IllegalStateException("Repository returned Initial state"))
                is CustomResult.Loading -> CustomResult.Failure(IllegalStateException("Repository returned Loading state"))
                is CustomResult.Progress -> CustomResult.Failure(IllegalStateException("Repository returned Progress state"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // 새로운 메서드: 클라이언트가 생성한 Message 객체를 직접 저장
    suspend operator fun invoke(message: Message): CustomResult<Message, Exception> {
        return try {
            when (val saveResult = messageRepository.save(message)) {
                is CustomResult.Success -> CustomResult.Success(message)
                is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                is CustomResult.Initial -> CustomResult.Failure(IllegalStateException("Repository returned Initial state"))
                is CustomResult.Loading -> CustomResult.Failure(IllegalStateException("Repository returned Loading state"))
                is CustomResult.Progress -> CustomResult.Failure(IllegalStateException("Repository returned Progress state"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}