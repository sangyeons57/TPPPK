package com.example.domain.usecase.auth

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.event.user.UserAccountWithdrawnEvent
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for processing user membership withdrawal.
 * This involves anonymizing user data and signing the user out,
 * but does not delete the user's auth record.
 */
interface WithdrawMembershipUseCase {
    /**
     * Executes the withdrawal process.
     * @return A [CustomResult] indicating success or failure.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception>
}

/**
 * Implementation for [WithdrawMembershipUseCase].
 * This use case processes user membership withdrawal by anonymizing user data in Firestore,
 * marking the account as withdrawn, and then signing the user out.
 */
class WithdrawMembershipUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : WithdrawMembershipUseCase {

    override suspend operator fun invoke(): CustomResult<Unit, Exception> {
        Log.d("WithdrawMembershipUseCaseImpl", "Attempting user withdrawal process.")
        // 1. Get current user's UID
        val currentUserResult = authRepository.getCurrentUserSession()
        val session = when (currentUserResult) {
            is CustomResult.Success -> currentUserResult.data
            is CustomResult.Failure -> {
                Log.e("WithdrawMembershipUseCaseImpl", "Failed to get current user", currentUserResult.error)
                return CustomResult.Failure(currentUserResult.error ?: Exception("Failed to get current user information."))
            }
            else -> {
                Log.e("WithdrawMembershipUseCaseImpl", "Unknown error while fetching current user")
                return CustomResult.Failure(Exception("Unknown error fetching user information."))
            }
        }

        val uid = session.userId
        Log.d("WithdrawMembershipUseCaseImpl", "Current user UID: $uid. Proceeding with data anonymization.")

        // 2. Process user data withdrawal (anonymize in Firestore)
        when ( val userResult = userRepository.observe(DocumentId.from(uid)).first()) {
            is CustomResult.Success -> {
                val user = userResult.data as User
                user.markAsWithdrawn()
                val result = userRepository.save(user)
                return when (result) {
                    is CustomResult.Success -> {
                        Log.d(
                            "WithdrawMembershipUseCaseImpl",
                            "User data withdrawal successful for UID: $uid."
                        )
                        EventDispatcher.publish(user)
                        CustomResult.Success(Unit)
                    }
                    is CustomResult.Failure -> CustomResult.Failure(result.error)
                    else -> CustomResult.Failure(Exception("Unknown error during data processing."))
                }
            }
            is CustomResult.Failure -> {
                Log.e("WithdrawMembershipUseCaseImpl", "Failed to process user data withdrawal for UID: $uid.", userResult.error)
                return CustomResult.Failure(userResult.error)
            }
            else -> {
                 Log.e("WithdrawMembershipUseCaseImpl", "Unknown error during data processing for UID: $uid.")
                return CustomResult.Failure(Exception("Unknown error during data processing."))
            }
        }
    }
}
