package com.example.data.repository

import com.example.data.datasource.local.friend.FriendLocalDataSource
import com.example.data.datasource.remote.friend.FriendRemoteDataSource
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import com.example.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * FriendRepository 인터페이스 구현
 * 로컬 및 원격 데이터 소스를 활용하여 친구 관련 기능을 제공합니다.
 */
class FriendRepositoryImpl @Inject constructor(
    private val remoteDataSource: FriendRemoteDataSource,
    private val localDataSource: FriendLocalDataSource
) : FriendRepository {

    /**
     * 친구 목록 실시간 스트림
     * 서버에서 새로운 친구 목록이 동기화되면 로컬에도 저장합니다.
     */
    override fun getFriendsListStream(): Flow<List<Friend>> {
        // 원격 데이터를 가져와서 로컬에 캐싱
        return remoteDataSource.getFriendsListStream().onEach { friends ->
            // 원격에서 데이터가 변경되면 로컬에 저장
            localDataSource.saveFriends(friends)
        }
    }

    /**
     * 친구 목록 새로고침 (API 호출)
     * 명시적으로 서버에서 친구 목록을 가져옵니다.
     */
    override suspend fun fetchFriendsList(): Result<Unit> {
        return remoteDataSource.fetchFriendsList()
    }

    /**
     * 친구와의 DM 채널 ID 가져오기
     * 필요한 경우 새 채널을 생성합니다.
     */
    override suspend fun getDmChannelId(friendUserId: String): Result<String> {
        return remoteDataSource.getDmChannelId(friendUserId)
    }

    /**
     * 사용자 이름으로 친구 요청 보내기
     * 닉네임으로 사용자를 찾아 친구 요청을 보냅니다.
     */
    override suspend fun sendFriendRequest(username: String): Result<String> {
        return remoteDataSource.sendFriendRequest(username)
    }

    /**
     * 받은 친구 요청 목록 가져오기
     * 서버에서 요청 목록을 가져와 로컬에 캐싱합니다.
     */
    override suspend fun getFriendRequests(): Result<List<FriendRequest>> {
        val result = remoteDataSource.getFriendRequests()
        
        // 성공 시에만 로컬에 저장
        if (result.isSuccess) {
            result.getOrNull()?.let { requests ->
                localDataSource.saveFriendRequests(requests)
            }
        }
        
        return result
    }

    /**
     * 친구 요청 수락
     * 원격 서버에서 요청을 수락하고 로컬 데이터베이스를 업데이트합니다.
     */
    override suspend fun acceptFriendRequest(userId: String): Result<Unit> {
        val result = remoteDataSource.acceptFriendRequest(userId)
        
        // 성공 시에만 로컬 업데이트
        if (result.isSuccess) {
            // 친구 정보 가져오기
            val friendInfo = localDataSource.getFriendById(userId) ?: Friend(
                userId = userId,
                userName = "사용자", // 기본값
                status = "accepted",
                profileImageUrl = null
            )
            
            // 로컬 데이터베이스 업데이트
            localDataSource.acceptFriendRequest(userId, friendInfo)
        }
        
        return result
    }

    /**
     * 친구 요청 거절
     * 원격 서버에서 요청을 거절하고 로컬 데이터베이스를 업데이트합니다.
     */
    override suspend fun denyFriendRequest(userId: String): Result<Unit> {
        val result = remoteDataSource.denyFriendRequest(userId)
        
        // 성공 시에만 로컬 업데이트
        if (result.isSuccess) {
            localDataSource.deleteFriendRequest(userId)
        }
        
        return result
    }
}
