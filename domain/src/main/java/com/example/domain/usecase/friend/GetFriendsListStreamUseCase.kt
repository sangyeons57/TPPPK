package com.example.domain.usecase.friend

import com.example.domain.model.FriendRelationship
import com.example.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.Result

/**
 * 친구 관계 목록 스트림을 가져오는 UseCase
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class GetFriendRelationshipsStreamUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    /**
     * 친구 관계 목록 정보를 실시간 스트림으로 가져옵니다.
     *
     * @return Result<Flow<List<FriendRelationship>>> 친구 관계 목록 정보 Flow.
     */
    operator fun invoke(): Flow<Result<List<FriendRelationship>>> {
        return friendRepository.getFriendRelationshipsStream()
    }
} 