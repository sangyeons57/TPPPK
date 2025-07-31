package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain_repository.base.MessageRepository
import java.time.Instant
import javax.inject.Inject

class FetchPastMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        limit: Int = 50,
        beforeTimestamp: Instant? = null,
        useCache: Boolean = true
    ): CustomResult<List<Message>, Exception> {
        return try {
            // Use direct repository access - Firestore handles caching natively
            val result = messageRepository.findAll()
            val messages = when (result) {
                is CustomResult.Success -> {
                    val allMessages = result.data
                    if (beforeTimestamp != null) {
                        // Filter messages before the specified timestamp
                        allMessages
                            .filter { it.createdAt.isBefore(beforeTimestamp) }
                            .sortedByDescending { it.createdAt }
                            .take(limit)
                    } else {
                        // Get most recent messages
                        allMessages.sortedByDescending { it.createdAt }.take(limit)
                    }.map { it as Message }
                }

                else -> emptyList()
            }

            CustomResult.Success(messages.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}