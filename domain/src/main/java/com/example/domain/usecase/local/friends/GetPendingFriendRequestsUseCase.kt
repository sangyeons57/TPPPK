package com.example.domain.usecase.local.friends

import com.example.domain.model.Friend
import com.example.domain.repository.local.FriendLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetPendingFriendRequestsUseCase {
    operator fun invoke(userId: String): Flow<List<Friend>>
}

class GetPendingFriendRequestsUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository
) : GetPendingFriendRequestsUseCase {

    override operator fun invoke(userId: String): Flow<List<Friend>> {
        return TODO("특정 사용자의 대기 중인 친구 요청 목록을 실시간으로 관찰")
    }
} 