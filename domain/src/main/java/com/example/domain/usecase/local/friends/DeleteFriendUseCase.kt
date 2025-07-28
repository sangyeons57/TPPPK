package com.example.domain.usecase.local.friends

import com.example.domain.repository.local.FriendLocalRepository
import com.example.domain.util.CustomResult
import javax.inject.Inject

interface DeleteFriendUseCase {
    suspend operator fun invoke(userId: String, friendId: String): CustomResult<Unit, Exception>
}

class DeleteFriendUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository
) : DeleteFriendUseCase {

    override suspend operator fun invoke(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 데이터베이스에서 특정 친구 관계를 삭제")
    }
} 