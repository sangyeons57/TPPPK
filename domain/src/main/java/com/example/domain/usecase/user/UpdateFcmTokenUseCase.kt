package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Loading.getOrDefault
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * FCM 토큰 업데이트 유스케이스 인터페이스
 */
interface UpdateFcmTokenUseCase {
    /**
     * Updates the current user's FCM token for push notifications
     * @param token The new FCM token (null to remove token)
     */
    suspend operator fun invoke(token: String?): CustomResult<Unit, Exception>
}

/**
 * UpdateFcmTokenUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 * @param authRepository 인증 데이터 접근을 위한 Repository
 */
class UpdateFcmTokenUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : UpdateFcmTokenUseCase {

    /**
     * 유스케이스를 실행하여 사용자의 FCM 토큰을 업데이트합니다.
     * @param token 새로운 FCM 토큰 (null이면 토큰 제거)
     * @return CustomResult<Unit, Exception> 업데이트 처리 결과
     */
    override suspend fun invoke(token: String?): CustomResult<Unit, Exception> {
        val session = authRepository.getCurrentUserSession().getOrDefault(null)
            ?: return CustomResult.Failure(Exception("User not logged in"))
            
        val userRes = userRepository.findById(DocumentId.from(session.userId))
        if (userRes is CustomResult.Failure) {
            return CustomResult.Failure(userRes.error)
        } else if (userRes !is CustomResult.Success) {
            return CustomResult.Failure(Exception("User not found"))
        }

        val user = userRes.data as User
        val fcmToken = token?.let { UserFcmToken(it) }
        
        user.updateFcmToken(fcmToken)
        
        return when (val saveRes = userRepository.save(user)) {
            is CustomResult.Success -> {
                EventDispatcher.publish(user)
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> CustomResult.Failure(saveRes.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(saveRes.progress)
        }
    }
}