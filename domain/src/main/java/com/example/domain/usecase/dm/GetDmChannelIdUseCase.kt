package com.example.domain.usecase.dm

import com.example.domain.repository.DmRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 특정 사용자와의 DM 채널 ID를 가져오는 UseCase
 * 
 * @property dmRepository DM 채널 관련 기능을 제공하는 Repository
 */
class GetDmChannelIdUseCase @Inject constructor(
    private val dmRepository: DmRepository
) {
    /**
     * 특정 사용자와의 DM 채널 ID를 가져옵니다.
     *
     * @param targetUserId 상대방 사용자 ID
     * @return 성공 시 채널 ID (없으면 null)가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(targetUserId: String): Result<String?> {
        if (targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Target user ID cannot be blank."))
        }
        return dmRepository.getDmChannelId(targetUserId)
    }
} 