package com.example.data.repository

import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource // 사용자 검색 및 정보 업데이트 시 필요
import com.example.data.model._remote.FriendRequestDTO
import com.example.data.model.mapper.toDomain // DTO -> Domain
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import com.example.domain.model.User
import com.example.domain.repository.FriendRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource // 사용자 검색 등에 사용
    // private val friendMapper: FriendMapper // 개별 매퍼 사용시
) : FriendRepository {

    override fun getFriendsStream(currentUserId: String): Flow<Result<List<Friend>>> {
        return friendRemoteDataSource.observeFriends(currentUserId).map { result -> // DataSource 함수명 확인 필요
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override fun getFriendRequestsStream(currentUserId: String): Flow<Result<List<FriendRequest>>> {
        return friendRemoteDataSource.observeFriendRequests(currentUserId).map { result -> // DataSource 함수명 확인 필요
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() } // FriendRequestDTO -> FriendRequest
            }
        }
    }

    override suspend fun sendFriendRequest(senderId: String, receiverNickname: String): Result<Unit> = resultTry {
        // 1. 닉네임으로 사용자 검색하여 receiverId 얻기
        val searchResult = userRemoteDataSource.searchUsersByName(receiverNickname, limit = 1).getOrThrow()
        val receiverUserDto = searchResult.firstOrNull() 
            ?: throw NoSuchElementException(\
User
with
nickname
\$receiverNickname
not
found.\)
        
        if (receiverUserDto.id == senderId) {
            throw IllegalArgumentException(\Cannot
send
friend
request
to
oneself.\)
        }

        // TODO: 이미 친구인지, 이미 요청을 보냈는지 등을 DataSource 레벨 또는 여기서 확인 필요
        // friendRemoteDataSource.checkExistingRelationship(senderId, receiverUserDto.id)

        val requestDto = FriendRequestDTO(
            // id는 Firestore에서 자동 생성
            senderId = senderId,
            receiverId = receiverUserDto.id!!, // UserDTO의 id는 non-null이라고 가정
            status = \PENDING\, // FriendRequestStatus.PENDING.name
            createdAt = Timestamp.now()
        )
        friendRemoteDataSource.sendFriendRequest(requestDto).getOrThrow()
    }

    override suspend fun acceptFriendRequest(friendRequestId: String, currentUserId: String): Result<Unit> = resultTry {
        // currentUserId는 요청을 받은 사람 (receiverId) 이어야 함. DataSource에서 검증.
        friendRemoteDataSource.acceptFriendRequest(friendRequestId).getOrThrow()
    }

    override suspend fun declineFriendRequest(friendRequestId: String, currentUserId: String): Result<Unit> = resultTry {
        // currentUserId는 요청을 받은 사람 (receiverId) 이어야 함. DataSource에서 검증.
        friendRemoteDataSource.declineFriendRequest(friendRequestId).getOrThrow()
    }

    override suspend fun removeFriend(currentUserId: String, friendId: String): Result<Unit> = resultTry {
        friendRemoteDataSource.removeFriend(currentUserId, friendId).getOrThrow()
    }

    override suspend fun searchUsersForFriend(nickname: String, currentUserId: String): Result<List<User>> = resultTry {
        val userDtoList = userRemoteDataSource.searchUsersByName(nickname, limit = 10).getOrThrow() // 적절한 limit 설정
        // TODO: 이미 친구이거나, 요청을 보냈거나, 자신인 경우 필터링
        userDtoList
            .filter { it.id != currentUserId } // 자기 자신 제외
            .map { it.toDomain() } // UserDTO -> User
    }
}
