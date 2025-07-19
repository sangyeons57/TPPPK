package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.base.MessageRepository
import com.google.firebase.firestore.Source
import javax.inject.Inject

class FetchPastMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        limit: Int = 50,
        useCache: Boolean = true
    ): CustomResult<List<Message>, Exception> {
        return try {
            val source = if (useCache) Source.DEFAULT else Source.SERVER
            
            when (val result = messageRepository.findAll(source)) {
                is CustomResult.Success -> {
                    val messages = result.data.filterIsInstance<Message>()
                        .sortedByDescending { it.createdAt }
                        .take(limit)
                    CustomResult.Success(messages)
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