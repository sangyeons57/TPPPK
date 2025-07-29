package com.example.domain.usecase.local.friends

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.FriendLocalRepository
import javax.inject.Inject

interface AcceptFriendRequestLocalUseCase {
    suspend operator fun invoke(friendId: String): CustomResult<Unit, Exception>
}

class AcceptFriendRequestLocalUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository,
    private val authRepository: AuthRepository
) : AcceptFriendRequestLocalUseCase {

    override suspend operator fun invoke(friendId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 친구 요청을 수락")
    }
} 