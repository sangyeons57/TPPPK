package com.example.domain.usecase.local.friends

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.FriendLocalRepository
import javax.inject.Inject

interface SendFriendRequestLocalUseCase {
    suspend operator fun invoke(targetUserId: String): CustomResult<Unit, Exception>
}

class SendFriendRequestLocalUseCaseImpl @Inject constructor(
    private val friendLocalRepository: FriendLocalRepository,
    private val authRepository: AuthRepository
) : SendFriendRequestLocalUseCase {

    override suspend operator fun invoke(targetUserId: String): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에 친구 요청을 저장")
    }
} 