package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.core_common.result.convertCustomResultListType
import com.example.domain.model.base.Message
import com.example.domain_repository.base.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessagesStreamUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(): Flow<CustomResult<List<Message>, Exception>> {
        return messageRepository.observeAll()
            .map { result ->
                convertCustomResultListType<Message>(result)
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