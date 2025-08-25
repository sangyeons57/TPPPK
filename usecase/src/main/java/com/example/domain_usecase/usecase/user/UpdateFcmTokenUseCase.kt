package com.example.domain_usecase.usecase.user

import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Loading.getOrDefault
import com.example.domain.event.EventDispatcher
import com.example.domain.event.user.UserFcmTokenUpdatedEvent
import com.example.domain.model.base.User
import com.example.domain.vo.DocumentId
import com.example.domain.vo.user.UserFcmToken
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * FCM 토큰 업데이트 유스케이스 인터페이스
 */
interface UpdateFcmTokenUseCase {
    /**
     * Updates the current user's FCM token for push notifications.
     * Retrieves the current device token internally from FirebaseMessaging.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception>

    /**
     * Updates the current user's FCM token for push notifications.
     * Uses the provided token instead of fetching from FirebaseMessaging.
     */
    suspend operator fun invoke(fcmToken: String): CustomResult<Unit, Exception>
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
     * FirebaseMessaging에서 자동으로 토큰을 조회합니다.
     * @return CustomResult<Unit, Exception> 업데이트 처리 결과
     */
    override suspend fun invoke(): CustomResult<Unit, Exception> {
        authRepository.getCurrentUserSession().getOrDefault(null)
            ?: return CustomResult.Failure(Exception("User not logged in"))

        val token = kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
        if (token == null) {
            return CustomResult.Failure(Exception("Unable to fetch FCM token"))
        }

        return invoke(token)
    }

    /**
     * 유스케이스를 실행하여 사용자의 FCM 토큰을 업데이트합니다.
     * 제공된 토큰을 사용합니다.
     * @param fcmToken 새로운 FCM 토큰
     * @return CustomResult<Unit, Exception> 업데이트 처리 결과
     */
    override suspend fun invoke(fcmToken: String): CustomResult<Unit, Exception> {
        val session = authRepository.getCurrentUserSession().getOrDefault(null)
            ?: return CustomResult.Failure(Exception("User not logged in"))

        val userRes = userRepository.findById(DocumentId.from(session.userId))
        if (userRes is CustomResult.Failure) {
            return CustomResult.Failure(userRes.error)
        } else if (userRes !is CustomResult.Success) {
            return CustomResult.Failure(Exception("User not found"))
        }

        val user = userRes.data as User
        user.updateFcmToken(UserFcmToken(fcmToken))
        
        return when (val saveRes = userRepository.save(user)) {
            is CustomResult.Success -> {
                EventDispatcher.publish(user)
                EventDispatcher.publish(UserFcmTokenUpdatedEvent(session.userId.value))
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> CustomResult.Failure(saveRes.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(saveRes.progress)
        }
    }
}
