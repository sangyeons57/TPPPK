package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain_repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * 친구 관계 및 친구 요청 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface FriendRepository : DefaultRepository<Friend> {

    suspend fun findFriendsByUserId(userId: String): CustomResult<List<Friend>, Exception>
    
    suspend fun findFriendRequestsByUserId(userId: String): CustomResult<List<Friend>, Exception>
    
    suspend fun findFriendByUserIdAndFriendId(userId: String, friendId: String): CustomResult<Friend?, Exception>
    
    suspend fun searchFriendsByUsername(username: String): CustomResult<List<Friend>, Exception>
    
    fun observeFriendRequests(userId: String): Flow<CustomResult<List<Friend>, Exception>>
    
    fun observeFriendsList(userId: String): Flow<CustomResult<List<Friend>, Exception>>
    
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): CustomResult<Unit, Exception>
    
    suspend fun acceptFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception>
    
    suspend fun declineFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception>
    
    suspend fun blockUser(userId: String, friendId: String): CustomResult<Unit, Exception>
    
    suspend fun removeFriend(userId: String, friendId: String): CustomResult<Unit, Exception>
}
