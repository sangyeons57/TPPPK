package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.remote.ChatCacheRepository
import java.time.Instant
import javax.inject.Inject

class FetchPastMessagesUseCase @Inject constructor(
    private val chatCacheRepository: ChatCacheRepository
) {
    suspend operator fun invoke(
        limit: Int = 50,
        beforeTimestamp: Instant? = null,
        useCache: Boolean = true
    ): CustomResult<List<Message>, Exception> {
        return try {
            val messages = if (beforeTimestamp != null) {
                // Load more past messages before the specified timestamp
                chatCacheRepository.loadMoreMessages(beforeTimestamp, limit)
            } else {
                // Get messages with sync (cache-first approach)
                chatCacheRepository.getMessagesWithSync(limit)
            }

            CustomResult.Success(messages.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}