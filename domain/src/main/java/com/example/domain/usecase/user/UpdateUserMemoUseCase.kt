package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.UserRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.base.User // Required for User.changeMemo and userRepository.save
import javax.inject.Inject

/**
 * Use case for updating the current user's memo (status message).
 */
interface UpdateUserMemoUseCase {
    /**
     * Invokes the use case to update the user's memo.
     *
     * @param newMemo The new memo string to set.
     * @return A [CustomResult] indicating success (Unit) or failure (Exception).
     */
    suspend operator fun invoke(newMemo: UserMemo): CustomResult<String, Exception>
}

/**
 * Implementation of [UpdateUserMemoUseCase] for updating the user's memo.
 */
class UpdateUserMemoUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : UpdateUserMemoUseCase {

    /**
     * Invokes the use case to update the user's memo.
     *
     * @param newMemo The new memo string to set.
     * @return A [CustomResult] indicating success (Unit) or failure (Exception).
     */
    override suspend operator fun invoke(newMemo: UserMemo): CustomResult<String, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        val userId = when (sessionResult) {
            is CustomResult.Success -> sessionResult.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(sessionResult.error)
            else -> return CustomResult.Failure(Exception("User session check in progress or uninitialized"))
        }

        val userResult = userRepository.findById(userId)
        return when (userResult) {
            is CustomResult.Success -> {
                val user = userResult.data
                user.changeMemo(newMemo)
                userRepository.save(user)
            }
            is CustomResult.Failure -> CustomResult.Failure(userResult.error)
            else -> CustomResult.Failure(Exception("Failed to retrieve user information: repository in unexpected state"))
        }
    }

}
