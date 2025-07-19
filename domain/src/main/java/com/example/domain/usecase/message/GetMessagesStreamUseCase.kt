package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.base.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessagesStreamUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(): Flow<CustomResult<List<Message>, Exception>> {
        return messageRepository.observeAll().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val messages = result.data.filterIsInstance<Message>()
                        .sortedByDescending { it.createdAt }
                    CustomResult.Success(messages)
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Initial -> CustomResult.Failure(IllegalStateException("Repository returned Initial state"))
                is CustomResult.Loading -> CustomResult.Failure(IllegalStateException("Repository returned Loading state"))
                is CustomResult.Progress -> CustomResult.Failure(IllegalStateException("Repository returned Progress state"))
            }
        }
    }
}