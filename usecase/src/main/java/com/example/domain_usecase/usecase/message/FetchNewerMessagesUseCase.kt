package com.example.domain_usecase.usecase.message

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.model.base.Message
import com.example.domain_repository.base.MessageRepository
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
        return resultTry {
            // Use direct repository access - Firestore handles real-time updates
            val result = messageRepository.findAll()
            return@resultTry when (result) {
                is CustomResult.Success -> {
                    result.data
                        .map { it as Message }
                        .filter { it.createdAt.isAfter(afterTimestamp) }
                        .sortedByDescending { it.createdAt }
                        .take(limit)
                }

                else -> emptyList()
            }
        }
    }
}