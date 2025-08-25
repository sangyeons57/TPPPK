package com.example.domain_usecase.usecase.dev

import com.example.core_common.result.CustomResult
import com.example.domain_repository.base.MessageRepository
import javax.inject.Inject

/**
 * 개발 편의용: Room에 저장된 로컬 캐시(메시지, OutBox, 동기화 메타데이터)를 전부 삭제합니다.
 */
class ClearAllRoomCacheUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * 모든 메시지/아웃박스/동기화 메타데이터를 삭제합니다.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception> {
        try {
            messageRepository.clearAllCache()

            return CustomResult.Success(Unit)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}

