package com.example.domain_usecase.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.sync.SyncCursorStore
import javax.inject.Inject

/**
 * 동기화 커서를 null로 리셋하여 전체 동기화를 강제하도록 만드는 유즈케이스
 */
class ResetSyncCursorUseCase @Inject constructor(
    private val cursorStore: SyncCursorStore,
) {
    /**
     * 주어진 스트림의 커서를 null로 저장 (다음 동기화에서 처음부터 시작)
     */
    suspend operator fun invoke(stream: String): CustomResult<Unit, Exception> =
        try {
            cursorStore.saveCursor(stream, null)
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }

    /**
     * 메시지 채널 스트림 커서 리셋 헬퍼
     */
    suspend fun resetMessagesChannel(channelId: String): CustomResult<Unit, Exception> =
        invoke("messages-$channelId")
}

