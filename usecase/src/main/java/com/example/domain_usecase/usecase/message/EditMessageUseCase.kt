package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.vo.DocumentId
import com.example.domain.vo.message.MessagePayload
import com.example.domain_repository.base.MessageRepository
import javax.inject.Inject

class EditMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        messageId: DocumentId,
        newPayload: MessagePayload
    ): CustomResult<Message, Exception> {
        return try {
            when (val findResult = messageRepository.findById(messageId)) {
                is CustomResult.Success -> {
                    val message = findResult.data as? Message
                        ?: return CustomResult.Failure(IllegalStateException("Message not found"))

                    message.updatePayload(newPayload)
                    
                    when (val saveResult = messageRepository.save(message)) {
                        is CustomResult.Success -> CustomResult.Success(message)
                        is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                        is CustomResult.Initial -> CustomResult.Failure(IllegalStateException("Repository returned Initial state"))
                        is CustomResult.Loading -> CustomResult.Failure(IllegalStateException("Repository returned Loading state"))
                        is CustomResult.Progress -> CustomResult.Failure(IllegalStateException("Repository returned Progress state"))
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(findResult.error)
                is CustomResult.Initial -> CustomResult.Failure(IllegalStateException("Repository returned Initial state"))
                is CustomResult.Loading -> CustomResult.Failure(IllegalStateException("Repository returned Loading state"))
                is CustomResult.Progress -> CustomResult.Failure(IllegalStateException("Repository returned Progress state"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}