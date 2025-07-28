package com.example.domain.usecase.local.messages

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.MessageLocalRepository
import javax.inject.Inject

interface EditMessageLocalUseCase {
    suspend operator fun invoke(
        messageId: DocumentId,
        newContent: String
    ): CustomResult<Message, Exception>
}

class EditMessageLocalUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : EditMessageLocalUseCase {

    override suspend operator fun invoke(
        messageId: DocumentId,
        newContent: String
    ): CustomResult<Message, Exception> {
        return TODO("로컬 저장소에서 메시지 내용 수정")
    }
} 