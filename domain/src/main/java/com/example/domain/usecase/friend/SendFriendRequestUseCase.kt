package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.FriendRepository
import javax.inject.Inject
import kotlin.Result

interface SendFriendRequestUseCase {
    suspend operator fun invoke(targetUserId: String): CustomResult<Unit, Exception>
}
/**
 * 친구 요청을 보내는 UseCase
 * 
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class SendFriendRequestUseCaseImpl @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
): SendFriendRequestUseCase {
    /**
     * 특정 사용자에게 친구 요청을 보냅니다.
     *
     * @param targetUserId 친구 요청을 보낼 대상 사용자의 ID.
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러 정보가 포함된 Result.
     */
    override suspend operator fun invoke(targetUserId: String): CustomResult<Unit, Exception> {
        val session = authRepository.getCurrentUserSession()
        when (session) {
            is CustomResult.Success -> {
                return friendRepository.sendFriendRequest(session.data, targetUserId)
            }
            else -> {
                return CustomResult.Failure(Exception("User not logged in"))
            }
        }
    }
} 