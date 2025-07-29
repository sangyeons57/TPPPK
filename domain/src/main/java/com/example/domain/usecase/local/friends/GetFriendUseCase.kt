package com.example.domain.usecase.local.friends

import com.example.domain.model.Friend
import com.example.domain.repository.local.FriendLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetFriendUseCase {
    operator fun invoke(userId: String, friendId: String): Flow<Friend?>
}

class GetFriendUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository
) : GetFriendUseCase {

    override operator fun invoke(userId: String, friendId: String): Flow<Friend?> {
        return TODO("특정 친구 관계 정보를 실시간으로 관찰")
    }
} 