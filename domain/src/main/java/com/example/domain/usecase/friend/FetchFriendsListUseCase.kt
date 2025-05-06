package com.example.domain.usecase.friend

import com.example.domain.repository.FriendRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 친구 관계 목록을 수동으로 새로고침하는 UseCase
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class RefreshFriendRelationshipsUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 친구 관계 목록을 새로고침합니다.
     *
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(): Result<Unit> {
        return friendRepository.refreshFriendRelationships()
    }
} 