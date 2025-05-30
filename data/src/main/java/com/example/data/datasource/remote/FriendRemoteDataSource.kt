
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.FriendDTO
import kotlinx.coroutines.flow.Flow

interface FriendRemoteDataSource {

    /**
     * 현재 로그인한 사용자의 친구("accepted" 상태) 목록을 실시간으로 관찰합니다.
     */
    fun observeFriends(): Flow<List<FriendDTO>>

    /**
     * 다른 사용자에게 친구 요청을 보냅니다.
     * @param friendId 요청을 보낼 사용자의 ID (이 ID가 상대방의 friends 컬렉션 내 문서 ID가 됨)
     * @param myName 내 이름 (상대방의 friends 컬렉션 내 friendName 필드에 저장될 값)
     * @param myProfileImageUrl 내 프로필 이미지 URL (상대방의 friends 컬렉션 내 friendProfileImageUrl 필드에 저장될 값)
     */
    suspend fun requestFriend(
        friendId: String,
        myName: String,
        myProfileImageUrl: String?
    ): CustomResult<Unit, Exception>

    /**
     * 친구 요청을 수락합니다.
     * @param requesterId 요청을 보낸 친구의 ID (내 friends 컬렉션 내 문서 ID)
     */
    suspend fun acceptFriendRequest(requesterId: String): CustomResult<Unit, Exception>

    /**
     * 친구를 삭제하거나 친구 요청을 거절합니다.
     * @param friendId 삭제/거절할 친구의 ID (내 friends 컬렉션 내 문서 ID)
     */
    suspend fun removeOrDenyFriend(friendId: String): CustomResult<Unit, Exception>
}

