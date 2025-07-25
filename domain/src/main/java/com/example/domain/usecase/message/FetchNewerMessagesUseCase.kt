package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.base.ChatCacheRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 특정 시간 이후의 새로운 메시지들을 가져오는 UseCase
 * 양방향 로딩을 위해 사용됩니다.
 */
class FetchNewerMessagesUseCase @Inject constructor(
    private val chatCacheRepository: ChatCacheRepository
) {
    suspend operator fun invoke(
        afterTimestamp: Instant,
        limit: Int = 50,
        useCache: Boolean = true
    ): CustomResult<List<Message>, Exception> {
        return try {
            // Force incremental sync to get newer messages
            chatCacheRepository.syncChannelIncremental(forceSync = !useCache)

            // Get messages with sync and filter for newer messages
            val allMessages =
                chatCacheRepository.getMessagesWithSync(limit * 2) // Get more to filter
            val newerMessages = allMessages
                .filter { it.createdAt.isAfter(afterTimestamp) }
                .sortedByDescending { it.createdAt }
                .take(limit)

            CustomResult.Success(newerMessages)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}