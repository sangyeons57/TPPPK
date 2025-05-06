package com.example.domain.usecase.friend

import com.example.domain.model.FriendRelationship
import com.example.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

// TODO: FriendRelationship에 status 상수 정의 필요 (예: FriendRelationship.Status.PENDING_RECEIVED)
private const val PENDING_RECEIVED_STATUS = "pending_received" // 임시 상수

/**
 * 받은 친구 요청 목록(FriendRelationship) 스트림을 가져오는 UseCase.
 * 내부적으로 전체 친구 관계 스트림을 필터링합니다.
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository.
 */
class GetPendingFriendRequestsUseCase @Inject constructor( // 이름 변경
    private val friendRepository: FriendRepository // 또는 GetFriendRelationshipsStreamUseCase 주입
) {
    /**
     * 받은 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     *
     * @return Result<Flow<List<FriendRelationship>>> 받은 친구 요청 목록 스트림.
     */
    operator fun invoke(): Flow<Result<List<FriendRelationship>>> {
        return friendRepository.getFriendRelationshipsStream().map { result ->
            result.map { relationships ->
                relationships.filter { it.status == PENDING_RECEIVED_STATUS } // 실제 상수 사용 권장
            }
        }
    }
} 