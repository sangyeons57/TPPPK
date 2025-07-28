package com.example.domain.usecase.local.friends

import com.example.domain.model.Friend
import com.example.domain.repository.local.FriendLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetFriendsUseCase {
    operator fun invoke(userId: String): Flow<List<Friend>>
}

class GetFriendsUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository
) : GetFriendsUseCase {

    override operator fun invoke(userId: String): Flow<List<Friend>> {
        return TODO("특정 사용자의 친구 목록을 실시간으로 관찰")
    }
} 