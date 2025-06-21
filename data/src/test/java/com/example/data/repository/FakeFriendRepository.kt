package com.example.data.repository

import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import com.example.domain.repository.base.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * FriendRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 FriendRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeFriendRepository : FriendRepository {
    
    // 인메모리 데이터 저장소
    private val friends = ConcurrentHashMap<String, Friend>()
    private val friendRequests = ConcurrentHashMap<String, FriendRequest>()
    private val dmChannels = ConcurrentHashMap<String, String>() // userId -> channelId 매핑
    
    // 친구 목록 Flow
    private val friendsListFlow = MutableStateFlow<List<Friend>>(emptyList())
    
    // 에러 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 친구 추가
     */
    fun addFriend(friend: Friend) {
        friends[friend.userId] = friend
        updateFriendsListFlow()
    }
    
    /**
     * 테스트를 위해 여러 친구 추가
     */
    fun addFriends(friendsList: List<Friend>) {
        friendsList.forEach { friend ->
            friends[friend.userId] = friend
        }
        updateFriendsListFlow()
    }
    
    /**
     * 테스트를 위해 친구 삭제
     */
    fun removeFriend(userId: String) {
        friends.remove(userId)
        updateFriendsListFlow()
    }
    
    /**
     * 테스트를 위해 모든 친구 초기화
     */
    fun clearFriends() {
        friends.clear()
        updateFriendsListFlow()
    }
    
    /**
     * 테스트를 위해 친구 요청 추가
     */
    fun addFriendRequest(request: FriendRequest) {
        friendRequests[request.userId] = request
    }
    
    /**
     * 테스트를 위해 친구 요청 삭제
     */
    fun removeFriendRequest(userId: String) {
        friendRequests.remove(userId)
    }
    
    /**
     * 테스트를 위해 모든 친구 요청 초기화
     */
    fun clearFriendRequests() {
        friendRequests.clear()
    }
    
    /**
     * 테스트를 위해 DM 채널 ID 설정
     */
    fun setDmChannelId(userId: String, channelId: String) {
        dmChannels[userId] = channelId
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }
    
    /**
     * 에러 시뮬레이션 확인 및 처리
     */
    private fun <T> simulateErrorIfNeeded(): Result<T>? {
        return if (shouldSimulateError) {
            Result.failure(errorToSimulate)
        } else {
            null
        }
    }
    
    /**
     * 친구 목록 Flow 업데이트
     */
    private fun updateFriendsListFlow() {
        friendsListFlow.value = friends.values.toList()
    }
    
    // FriendRepository 인터페이스 구현
    
    override fun getFriendsListStream(): Flow<List<Friend>> {
        return friendsListFlow.asStateFlow()
    }
    
    override suspend fun fetchFriendsList(): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 실제 구현에서는 서버에서 데이터를 가져옴
        // 테스트에서는 이미 설정된 인메모리 데이터를 그대로 사용
        updateFriendsListFlow()
        
        return Result.success(Unit)
    }
    
    override suspend fun getDmChannelId(friendUserId: String): Result<String> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<String>()?.let { return it }
        
        // 사용자 존재 확인
        if (!friends.containsKey(friendUserId)) {
            return Result.failure(NoSuchElementException("Friend not found: $friendUserId"))
        }
        
        // DM 채널 ID 가져오기
        val channelId = dmChannels[friendUserId]
            ?: return Result.failure(NoSuchElementException("DM channel not found for user: $friendUserId"))
        
        return Result.success(channelId)
    }
    
    override suspend fun sendFriendRequest(username: String): Result<String> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<String>()?.let { return it }
        
        // 유효하지 않은 사용자 이름 검사
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be empty"))
        }
        
        // 이미 친구인 경우 검사
        val existingFriend = friends.values.find { it.userName == username }
        if (existingFriend != null) {
            return Result.failure(IllegalStateException("User is already a friend"))
        }
        
        // 친구 요청 생성 성공 메시지 반환
        return Result.success("친구 요청을 보냈습니다.")
    }
    
    override suspend fun getFriendRequests(): Result<List<FriendRequest>> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<List<FriendRequest>>()?.let { return it }
        
        // 친구 요청 목록 반환
        return Result.success(friendRequests.values.toList())
    }
    
    override suspend fun acceptFriendRequest(userId: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 요청 존재 확인
        val request = friendRequests[userId]
            ?: return Result.failure(NoSuchElementException("Friend request not found: $userId"))
        
        // 친구 추가
        val newFriend = Friend(
            userId = request.userId,
            userName = request.userName,
            status = "온라인", // 기본 상태 설정
            profileImageUrl = request.profileImageUrl
        )
        
        friends[userId] = newFriend
        updateFriendsListFlow()
        
        // 수락된 요청 제거
        friendRequests.remove(userId)
        
        return Result.success(Unit)
    }
    
    override suspend fun denyFriendRequest(userId: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 요청 존재 확인
        if (!friendRequests.containsKey(userId)) {
            return Result.failure(NoSuchElementException("Friend request not found: $userId"))
        }
        
        // 요청 제거
        friendRequests.remove(userId)
        
        return Result.success(Unit)
    }
} 