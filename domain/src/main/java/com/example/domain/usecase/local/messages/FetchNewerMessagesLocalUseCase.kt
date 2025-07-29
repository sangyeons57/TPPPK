package com.example.domain.usecase.local.messages

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.MessageLocalRepository
import javax.inject.Inject

interface FetchNewerMessagesLocalUseCase {
    suspend operator fun invoke(
        channelId: DocumentId,
        afterMessageId: DocumentId?,
        limit: Int = 20
    ): CustomResult<List<Message>, Exception>
}

class FetchNewerMessagesLocalUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : FetchNewerMessagesLocalUseCase {

    override suspend operator fun invoke(
        channelId: DocumentId,
        afterMessageId: DocumentId?,
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return TODO("로컬 저장소에서 최신 메시지들을 페이지네이션으로 조회")
    }
} 