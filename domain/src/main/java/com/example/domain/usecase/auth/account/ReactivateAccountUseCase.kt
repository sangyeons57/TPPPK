package com.example.domain.usecase.auth.account

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * 탈퇴한 계정을 재활성화하는 유스케이스입니다.
 */
class ReactivateAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        email: String,
        nickname: String,
        consentTimeStamp: Instant
    ): CustomResult<User, Exception> {
        //(TAG, "Attempting to reactivate account for email: $email")

        return when (val userResult = userRepository.observeByEmail(email).first()) {
            is CustomResult.Success -> {
                val userToReactivate = userResult.data
                // 1. Reactivate the withdrawn account
                userToReactivate.reactivateAccount()
                // 2. Update the nickname
                userToReactivate.changeName(UserName(nickname))
                // Note: consentTimeStamp is typically an initial agreement. 
                // If re-consent is needed, the User model would need a specific method or property update.
                // For now, we assume original consent is still valid or re-affirmation is handled elsewhere.

                when (val saveResult = userRepository.save(userToReactivate)) {
                    is CustomResult.Success -> {
                        //(TAG, "Account reactivated and user data saved for email: $email")
                        EventDispatcher.publish(userToReactivate)
                        CustomResult.Success(userToReactivate)
                    }
                    is CustomResult.Failure -> {
                        // Failed to save reactivated user data for email: $email
                        saveResult
                    }
                    else -> CustomResult.Failure(Exception("Unknown error during saving reactivated user."))
                }
            }
            is CustomResult.Failure -> {
                // Failed to find user for reactivation with email: $email
                userResult
            }
            else -> CustomResult.Failure(Exception("Unknown error during user lookup for reactivation."))
        }
    }
}
