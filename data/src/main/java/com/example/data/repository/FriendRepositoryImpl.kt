package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource // 사용자 검색 및 정보 업데이트 시 필요
import com.example.domain.model._new.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.example.domain.model.base.User
import com.example.domain.repository.FriendRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 친구 관련 기능을 제공하는 Repository 구현체
 * Firebase를 사용하여 친구 관계 데이터를 관리합니다.
 */
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource // 사용자 검색 등에 사용
) : FriendRepository {

    /**
     * 현재 사용자의 친구 목록을 실시간 스트림으로 가져옵니다.
     * 
     * @param currentUserId 현재 사용자 ID
     * @return 친구 목록을 담은 Flow
     */
    override fun getFriendsStream(currentUserId: String): Flow<CustomResult<List<Friend>, Exception>> {
        return friendRemoteDataSource.observeFriends(currentUserId).map { result -> 
            when (result) {
                is CustomResult.Success -> {
                    try {
                        val friends = result.data.map { dto ->
                            Friend(
                                friendUid = dto.id ?: "",
                                friendName = dto.name ?: "",
                                friendProfileImageUrl = dto.profileImageUrl,
                                status = FriendStatus.fromString(dto.status),
                                requestedAt = dto.requestedAt?.toDate()?.toInstant(),
                                acceptedAt = dto.acceptedAt?.toDate()?.toInstant()
                            )
                        }
                        CustomResult.Success(friends)
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in getFriendsStream"))
                }
            }
        }
    }

    /**
     * 현재 사용자에게 온 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     * 
     * @param currentUserId 현재 사용자 ID
     * @return 친구 요청 목록을 담은 Flow
     */
    override fun getFriendRequestsStream(currentUserId: String): Flow<CustomResult<List<FriendStatus>, Exception>> {
        return friendRemoteDataSource.observeFriendRequests(currentUserId).map { result -> 
            when (result) {
                is CustomResult.Success -> {
                    try {
                        val statuses = result.data.map { dto ->
                            FriendStatus.fromString(dto.status)
                        }
                        CustomResult.Success(statuses)
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in getFriendRequestsStream"))
                }
            }
        }
    }

    /**
     * 친구 요청을 보냅니다.
     * 
     * @param senderId 요청을 보내는 사용자 ID
     * @param receiverNickname 요청을 받을 사용자의 닉네임
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun sendFriendRequest(senderId: String, receiverNickname: String): CustomResult<Unit, Exception> {
        return try {
            // 1. 닉네임으로 사용자 검색하여 receiverId 얻기
            val searchResult = userRemoteDataSource.searchUsersByName(receiverNickname, limit = 1)
            
            when (searchResult) {
                is CustomResult.Success -> {
                    val receiverUser = searchResult.data.firstOrNull() 
                        ?: return CustomResult.Failure(Exception("User with nickname $receiverNickname not found"))
                    
                    if (receiverUser.uid == senderId) {
                        return CustomResult.Failure(Exception("Cannot send friend request to oneself"))
                    }

                    // 친구 요청 생성
                    val friend = Friend(
                        friendUid = receiverUser.uid,
                        friendName = receiverUser.name,
                        friendProfileImageUrl = receiverUser.profileImageUrl,
                        status = FriendStatus.PENDING,
                        requestedAt = java.time.Instant.now(),
                        acceptedAt = null
                    )
                    
                    // 요청 전송
                    val result = friendRemoteDataSource.sendFriendRequest(friend, senderId)
                    result
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(searchResult.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in sendFriendRequest"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 친구 요청을 수락합니다.
     * 
     * @param friendRequestId 친구 요청 ID
     * @param currentUserId 현재 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun acceptFriendRequest(friendRequestId: String, currentUserId: String): CustomResult<Unit, Exception> {
        return try {
            val result = friendRemoteDataSource.updateFriendStatus(
                friendId = friendRequestId,
                userId = currentUserId,
                newStatus = FriendStatus.ACCEPTED,
                acceptedAt = java.time.Instant.now()
            )
            result
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 친구 요청을 거절합니다.
     * 
     * @param friendRequestId 친구 요청 ID
     * @param currentUserId 현재 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun declineFriendRequest(friendRequestId: String, currentUserId: String): CustomResult<Unit, Exception> {
        return try {
            val result = friendRemoteDataSource.updateFriendStatus(
                friendId = friendRequestId,
                userId = currentUserId,
                newStatus = FriendStatus.DECLINED,
                acceptedAt = null
            )
            result
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 친구를 삭제합니다.
     * 
     * @param currentUserId 현재 사용자 ID
     * @param friendId 삭제할 친구 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun removeFriend(currentUserId: String, friendId: String): CustomResult<Unit, Exception> {
        return try {
            friendRemoteDataSource.removeFriend(currentUserId, friendId)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자를 닉네임으로 검색합니다.
     * 
     * @param nickname 검색할 닉네임
     * @param currentUserId 현재 사용자 ID
     * @return 검색된 사용자 목록
     */
    override suspend fun searchUsersForFriend(nickname: String, currentUserId: String): CustomResult<List<User>, Exception> {
        return try {
            val searchResult = userRemoteDataSource.searchUsersByName(nickname, limit = 10)
            
            when (searchResult) {
                is CustomResult.Success -> {
                    // 자기 자신 제외
                    val filteredUsers = searchResult.data.filter { it.uid != currentUserId }
                    CustomResult.Success(filteredUsers)
                }
                is CustomResult.Failure -> {
                    searchResult
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in searchUsersForFriend"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
