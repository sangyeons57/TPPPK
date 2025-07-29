package com.example.domain.usecase.local.friends

import com.example.domain.model.Friend
import com.example.domain.repository.local.FriendLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface InsertFriendUseCase {
    suspend operator fun invoke(friend: Friend): CustomResult<Unit, Exception>
}

class InsertFriendUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository
) : InsertFriendUseCase {

    override suspend operator fun invoke(friend: Friend): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에 친구 정보를 삽입하거나 갱신")
    }
} 