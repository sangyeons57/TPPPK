package com.example.domain.usecase.local.messages

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.MessageLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetMessagesStreamLocalUseCase {
    operator fun invoke(channelId: DocumentId): Flow<CustomResult<List<Message>, Exception>>
}

class GetMessagesStreamLocalUseCaseImpl @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) : GetMessagesStreamLocalUseCase {

    override operator fun invoke(channelId: DocumentId): Flow<CustomResult<List<Message>, Exception>> {
        return TODO("로컬 저장소에서 특정 채널의 메시지 목록을 실시간으로 관찰")
    }
} 