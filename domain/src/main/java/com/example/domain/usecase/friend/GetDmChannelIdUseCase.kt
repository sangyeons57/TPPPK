package com.example.domain.usecase.friend

import com.example.domain.repository.FriendRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 친구와의 DM 채널 ID를 가져오는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class GetDmChannelIdUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 특정 친구와의 DM 채널 ID를 가져옵니다.
     *
     * @param userId 친구의 사용자 ID
     * @return 성공 시 채널 ID가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(userId: String): Result<String> {
        return friendRepository.getDmChannelId(userId)
    }
} 