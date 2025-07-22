package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.base.MessageRepository
import com.google.firebase.firestore.Source
import java.time.Instant
import javax.inject.Inject

/**
 * 특정 시간 이후의 새로운 메시지들을 가져오는 UseCase
 * 양방향 로딩을 위해 사용됩니다.
 */
class FetchNewerMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        afterTimestamp: Instant,
        limit: Int = 50,
        useCache: Boolean = true
    ): CustomResult<List<Message>, Exception> {
        return try {
            val source = if (useCache) Source.DEFAULT else Source.SERVER
            
            when (val result = messageRepository.findAll(source)) {
                is CustomResult.Success -> {
                    // Client-side filtering for messages newer than the given timestamp
                    // TODO: Implement proper Firestore query with timestamp-based pagination
                    val newerMessages = result.data.filterIsInstance<Message>()
                        .filter { it.createdAt.isAfter(afterTimestamp) }
                        .sortedByDescending { it.createdAt }
                        .take(limit)
                    
                    CustomResult.Success(newerMessages)
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Initial -> CustomResult.Failure(IllegalStateException("Repository returned Initial state"))
                is CustomResult.Loading -> CustomResult.Failure(IllegalStateException("Repository returned Loading state"))
                is CustomResult.Progress -> CustomResult.Failure(IllegalStateException("Repository returned Progress state"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}