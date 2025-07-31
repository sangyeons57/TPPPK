package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.enum.EntityType
import com.example.domain.model.base.Message
import com.example.domain.model.sync.OutBox
import com.example.domain.model.sync.OutBoxPayload
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
import com.example.domain_repository.base.MessageRepository
import com.example.domain_repository.local.OutBoxRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val outBoxRepository: OutBoxRepository
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

            // 1. 메시지 저장
            when (val saveResult = messageRepository.save(message)) {
                is CustomResult.Success -> {
                    // 2. OutBox에 동기화 작업 추가
                    val operation = if (message.isNew) {
                        OutBox.OutBoxOperation.CREATE
                    } else {
                        OutBox.OutBoxOperation.UPDATE
                    }

                    val outBox = OutBox.create(
                        entityType = EntityType.MESSAGE,
                        entityId = message.id.value,
                        operation = operation,
                        payload = OutBoxPayload.create(message)
                    )

                    when (val outBoxResult = outBoxRepository.insert(outBox)) {
                        is CustomResult.Success -> CustomResult.Success(message)
                        is CustomResult.Failure -> CustomResult.Failure(outBoxResult.error)
                        else -> CustomResult.Failure(IllegalStateException("OutBox operation returned unexpected state"))
                    }
                }
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
            // 1. 메시지 저장
            when (val saveResult = messageRepository.save(message)) {
                is CustomResult.Success -> {
                    // 2. OutBox에 동기화 작업 추가
                    val operation = if (message.isNew) {
                        OutBox.OutBoxOperation.CREATE
                    } else {
                        OutBox.OutBoxOperation.UPDATE
                    }

                    val outBox = OutBox.create(
                        entityType = EntityType.MESSAGE,
                        entityId = message.id.value,
                        operation = operation,
                        payload = OutBoxPayload.create(message)
                    )

                    when (val outBoxResult = outBoxRepository.insert(outBox)) {
                        is CustomResult.Success -> CustomResult.Success(message)
                        is CustomResult.Failure -> CustomResult.Failure(outBoxResult.error)
                        else -> CustomResult.Failure(IllegalStateException("OutBox operation returned unexpected state"))
                    }
                }
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