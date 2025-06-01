package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource // 사용자 검색 및 정보 업데이트 시 필요
import com.example.data.model.remote.FriendDTO
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.example.domain.model.base.User
import com.example.domain.repository.FriendRepository
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
                            // Use toDomain() extension function instead of manual mapping
                            dto.toDomain()
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
                is CustomResult.Success<List<FriendDTO>> -> {
                    try {
                        val statuses = result.data.map { dto ->
                            // Use FriendStatus.valueOf with uppercase
                            try {
                                FriendStatus.valueOf(dto.status.uppercase())
                            } catch (e: Exception) {
                                FriendStatus.PENDING
                            }
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
        return resultTry {
            // 닉네임으로 사용자 검색 - limit 제한 추가
            val searchResult = userRemoteDataSource.searchUsersByName(receiverNickname, 10)
            
            when (searchResult) {
                is CustomResult.Success -> {
                    val receiverUser = searchResult.data.firstOrNull { it.name == receiverNickname }
                        ?: throw Exception("User with nickname $receiverNickname not found")
                    
                    // friendRemoteDataSource.requestFriend 메소드 사용
                    val result = friendRemoteDataSource.requestFriend(
                        friendId = receiverUser.uid,
                        myName = "Sender Name", // 여기에 실제 사용자 이름 필요
                        myProfileImageUrl = null // 여기에 실제 프로필 URL 필요
                    )
                    
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unknown error in sendFriendRequest"))
                    }
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(searchResult.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in sendFriendRequest"))
                }
            }
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
        return resultTry {
            // 수락은 친구 요청을 수락하는 것이므로 acceptFriendRequest 사용
            val result = friendRemoteDataSource.acceptFriendRequest(friendRequestId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown error in acceptFriendRequest"))
            }
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
        return resultTry {
            // 거절은 친구를 삭제하는 것과 같으므로 removeOrDenyFriend 사용
            val result = friendRemoteDataSource.removeOrDenyFriend(friendRequestId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown error in declineFriendRequest"))
            }
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
        return resultTry {
            // removeOrDenyFriend 메소드로 변경 (removeFriend 대신)
            val result = friendRemoteDataSource.removeOrDenyFriend(friendId)
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unknown error in removeFriend"))
            }
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
        return resultTry {
            // limit 인자 이름이 다른 경우 수정 (매개변수 이름 제거)
            val searchResult = userRemoteDataSource.searchUsersByName(nickname, 10)

            when (searchResult) {
                is CustomResult.Success -> {
                    // 자기 자신 제외
                    val filteredUsers = searchResult.data.map{ userDTO -> userDTO.toDomain() }.filter { it.uid != currentUserId }
                    return CustomResult.Success(filteredUsers)
                }
                is CustomResult.Failure -> {
                    throw searchResult.error
                }

                else -> {
                    throw Exception("Unknown error in searchUsersForFriend")
                }
            }
        }
    }
}
