package com.example.domain.usecase.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.remote.ChatCacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessagesStreamUseCase @Inject constructor(
    private val chatCacheRepository: ChatCacheRepository
) {
    operator fun invoke(): Flow<CustomResult<List<Message>, Exception>> {
        return chatCacheRepository.observeChannelMessages(50)
            .map { messages ->
                CustomResult.Success(messages.sortedByDescending { it.createdAt }) as CustomResult<List<Message>, Exception>
            }
            .catch { exception ->
                emit(
                    CustomResult.Failure(
                        exception as? Exception ?: Exception(
                            exception.message,
                            exception
                        )
                    )
                )
            }
    }
}