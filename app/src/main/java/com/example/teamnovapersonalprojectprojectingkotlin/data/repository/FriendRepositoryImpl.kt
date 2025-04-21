package com.example.teamnovapersonalprojectprojectingkotlin.data.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.Friend
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.FriendRequest
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.Result

class FriendRepositoryImpl @Inject constructor(
    // TODO: FriendApiService, FriendDao 등 주입
) : FriendRepository {

    override fun getFriendsListStream(): Flow<List<Friend>> {
        println("FriendRepositoryImpl: getFriendsListStream called (returning empty list flow)")
        return flowOf(emptyList()) // 빈 리스트 Flow 반환
    }

    override suspend fun fetchFriendsList(): Result<Unit> {
        println("FriendRepositoryImpl: fetchFriendsList called (returning success)")
        return Result.success(Unit)
    }

    override suspend fun getDmChannelId(friendUserId: String): Result<String> {
        println("FriendRepositoryImpl: getDmChannelId called for $friendUserId (returning failure)")
        return Result.failure(NotImplementedError("DM 채널 ID 조회 기능 구현 필요"))
    }

    override suspend fun sendFriendRequest(username: String): Result<String> {
        println("FriendRepositoryImpl: sendFriendRequest called for $username (returning success message)")
        return Result.success("친구 요청을 보냈습니다. (임시)")
    }

    override suspend fun getFriendRequests(): Result<List<FriendRequest>> {
        println("FriendRepositoryImpl: getFriendRequests called (returning empty list)")
        return Result.success(emptyList())
    }

    override suspend fun acceptFriendRequest(userId: String): Result<Unit> {
        println("FriendRepositoryImpl: acceptFriendRequest called for $userId (returning success)")
        return Result.success(Unit)
    }

    override suspend fun denyFriendRequest(userId: String): Result<Unit> {
        println("FriendRepositoryImpl: denyFriendRequest called for $userId (returning success)")
        return Result.success(Unit)
    }
}