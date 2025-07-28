package com.example.domain.usecase.local.messages

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.MessageLocalRepository
import javax.inject.Inject

interface SendMessageLocalUseCase {
    suspend operator fun invoke(
        channelId: DocumentId,
        content: String,
        attachments: List<String> = emptyList()
    ): CustomResult<Message, Exception>
}

class SendMessageLocalUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : SendMessageLocalUseCase {

    override suspend operator fun invoke(
        channelId: DocumentId,
        content: String,
        attachments: List<String>
    ): CustomResult<Message, Exception> {
        return TODO("로컬 저장소에 새 메시지를 저장하고 임시 ID 할당")
    }
} 