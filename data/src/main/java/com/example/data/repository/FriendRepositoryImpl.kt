package com.example.data.repository

import com.example.data.datasource.remote.friend.FriendRemoteDataSource
import com.example.data.model.mapper.FriendMapper
import com.example.domain.model.FriendRelationship
import com.example.domain.repository.FriendRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

/**
 * FriendRepository 인터페이스 구현
 * 로컬 및 원격 데이터 소스를 활용하여 친구 관련 기능을 제공합니다.
 */
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val firebaseAuth: FirebaseAuth,
    private val friendMapper: FriendMapper
) : FriendRepository {

    private fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * 친구 관계 목록 실시간 스트림
     * 서버에서 새로운 친구 관계 목록이 동기화되면 로컬에도 저장합니다.
     */
    override fun getFriendRelationshipsStream(): Flow<Result<List<FriendRelationship>>> {
        val currentUserId = getCurrentUserId()
            ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("User not logged in.")))

        return friendRemoteDataSource.getFriendRelationshipsStream(currentUserId).map { result ->
            result.mapCatching { dtoListWithIds -> // Result 내에서 map 수행
                dtoListWithIds.map { (friendId, dto) ->
                    friendMapper.mapToDomain(dto, currentUserId)
                }
            }
        }
    }

    /**
     * 친구 관계 새로고침 (API 호출)
     * 명시적으로 서버에서 친구 관계 목록을 가져옵니다.
     */
    override suspend fun refreshFriendRelationships(): Result<Unit> {
        // Firestore 실시간 리스너를 사용하므로, 명시적 새로고침은 UI단의 스트림 재구독이나
        // DataSource에 별도 one-time fetch 구현 필요. 여기서는 성공으로 간주.
        // TODO: 필요시 DataSource에 fetch 기능 추가하고 호출
        return Result.success(Unit)
    }

    /**
     * 사용자 이름으로 친구 요청 보내기
     * 닉네임으로 사용자를 찾아 친구 요청을 보냅니다.
     */
    override suspend fun sendFriendRequest(targetUserId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        if (currentUserId == targetUserId) {
            return Result.failure(IllegalArgumentException("Cannot send friend request to oneself."))
        }
        return friendRemoteDataSource.sendFriendRequest(currentUserId, targetUserId)
    }

    /**
     * 친구 요청 수락
     * 원격 서버에서 요청을 수락하고 로컬 데이터베이스를 업데이트합니다.
     */
    override suspend fun acceptFriendRequest(requesterId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        // TODO: 로컬 데이터 업데이트 로직 (필요하다면)
        return friendRemoteDataSource.acceptFriendRequest(currentUserId, requesterId)
    }

    /**
     * 친구 요청 거절
     * 원격 서버에서 요청을 거절하고 로컬 데이터베이스를 업데이트합니다.
     */
    override suspend fun removeOrDenyFriend(friendId: String): Result<Unit> {
        val currentUserId = getCurrentUserId()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        // TODO: 로컬 데이터 업데이트 로직 (필요하다면)
        return friendRemoteDataSource.removeOrDenyFriend(currentUserId, friendId)
    }
}
