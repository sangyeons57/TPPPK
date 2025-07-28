package com.example.domain.usecase.local.messages

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.MessageLocalRepository
import javax.inject.Inject

interface DeleteMessageLocalUseCase {
    suspend operator fun invoke(messageId: DocumentId): CustomResult<Unit, Exception>
}

class DeleteMessageLocalUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : DeleteMessageLocalUseCase {

    override suspend operator fun invoke(messageId: DocumentId): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 메시지 삭제 또는 삭제 마크")
    }
} 