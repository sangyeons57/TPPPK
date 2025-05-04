package com.example.data.datasource.local.friend

import com.example.data.db.dao.FriendDao
import com.example.data.model.local.FriendEntity
import com.example.data.model.local.FriendRequestEntity
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 친구 관련 로컬 데이터 소스 구현
 * Room 데이터베이스를 사용하여 친구 목록 및 친구 요청 관련 기능을 구현합니다.
 * @param friendDao 친구 관련 Room DAO
 */
class FriendLocalDataSourceImpl @Inject constructor(
    private val friendDao: FriendDao
) : FriendLocalDataSource {

    // 도메인 모델 -> 로컬 엔티티 변환 함수
    private fun Friend.toEntity(status: String = "accepted"): FriendEntity = FriendEntity(
        id = this.userId,
        nickname = this.userName,
        status = this.status,
        profileImageUrl = this.profileImageUrl,
        acceptedAt = null, // 필요시 파라미터로 전달
        lastUpdatedAt = LocalDateTime.now()
    )

    // 로컬 엔티티 -> 도메인 모델 변환 함수
    private fun FriendEntity.toDomain(): Friend = Friend(
        userId = this.id,
        userName = this.nickname,
        status = this.status,
        profileImageUrl = this.profileImageUrl
    )

    // 도메인 모델 -> 로컬 엔티티 변환 함수 (친구 요청)
    private fun FriendRequest.toEntity(): FriendRequestEntity = FriendRequestEntity(
        userId = userId,
        nickname = userName,
        profileImageUrl = profileImageUrl,
        timestamp = timestamp,
        cachedAt = LocalDateTime.now()
    )

    // 로컬 엔티티 -> 도메인 모델 변환 함수 (친구 요청)
    private fun FriendRequestEntity.toDomain(): FriendRequest = FriendRequest(
        userId = userId,
        userName = nickname,
        profileImageUrl = profileImageUrl,
        timestamp = timestamp
    )

    /**
     * 친구 목록 스트림을 가져옵니다.
     */
    override fun getFriendsStream(): Flow<List<Friend>> {
        return friendDao.getAllFriendsStream().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 친구 목록을 로컬 데이터베이스에 저장합니다.
     */
    override suspend fun saveFriends(friends: List<Friend>) {
        val entities = friends.map { it.toEntity() }
        friendDao.insertOrUpdateFriends(entities)
    }

    /**
     * 특정 친구 정보를 가져옵니다.
     */
    override suspend fun getFriendById(friendId: String): Friend? {
        return friendDao.getFriendById(friendId)?.toDomain()
    }

    /**
     * 특정 친구 정보를 저장합니다.
     */
    override suspend fun saveFriend(friend: Friend) {
        friendDao.insertOrUpdateFriend(friend.toEntity())
    }

    /**
     * 특정 친구 정보를 삭제합니다.
     */
    override suspend fun deleteFriend(friendId: String) {
        friendDao.deleteFriend(friendId)
    }

    /**
     * 모든 친구 정보를 삭제합니다.
     */
    override suspend fun deleteAllFriends() {
        friendDao.deleteAllFriends()
    }

    /**
     * 친구 요청 목록을 가져옵니다.
     */
    override suspend fun getFriendRequests(): List<FriendRequest> {
        return friendDao.getAllFriendRequests().map { it.toDomain() }
    }

    /**
     * 친구 요청 목록을 저장합니다.
     */
    override suspend fun saveFriendRequests(requests: List<FriendRequest>) {
        val entities = requests.map { it.toEntity() }
        friendDao.insertOrUpdateFriendRequests(entities)
    }

    /**
     * 친구 요청을 저장합니다.
     */
    override suspend fun saveFriendRequest(request: FriendRequest) {
        friendDao.insertOrUpdateFriendRequest(request.toEntity())
    }

    /**
     * 친구 요청을 삭제합니다.
     */
    override suspend fun deleteFriendRequest(userId: String) {
        friendDao.deleteFriendRequest(userId)
    }

    /**
     * 모든 친구 요청을 삭제합니다.
     */
    override suspend fun deleteAllFriendRequests() {
        friendDao.deleteAllFriendRequests()
    }

    /**
     * 친구 요청이 수락되었을 때 처리합니다.
     */
    override suspend fun acceptFriendRequest(userId: String, friend: Friend) {
        val requestEntity = friendDao.getFriendById(userId)?.let { 
            FriendRequestEntity(
                userId = userId,
                nickname = it.nickname,
                profileImageUrl = it.profileImageUrl,
                timestamp = it.acceptedAt ?: LocalDateTime.now(),
                cachedAt = LocalDateTime.now()
            )
        } ?: return
        
        friendDao.acceptFriendRequest(requestEntity, friend.toEntity())
    }
} 