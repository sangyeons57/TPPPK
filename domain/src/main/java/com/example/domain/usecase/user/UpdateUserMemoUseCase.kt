package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Loading.getOrDefault
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.base.AuthRepository
import com.example.domain.model.vo.user.UserMemo
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
    suspend operator fun invoke(newMemo: UserMemo): CustomResult<User, Exception>
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
    override suspend operator fun invoke(newMemo: UserMemo): CustomResult<User, Exception> {
        val session = authRepository.getCurrentUserSession().getOrDefault(null)
            ?: return CustomResult.Failure(Exception("User not logged in"))

        val user = when (val userResult = userRepository.findById(DocumentId.from(session.userId))) {
            is CustomResult.Success -> userResult.data as User
            is CustomResult.Failure -> return CustomResult.Failure(userResult.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(userResult.progress)
        }
        user.changeMemo(newMemo)

        return when (val userResult =userRepository.save(user)){
            is CustomResult.Success -> {
                EventDispatcher.publish(user)
                CustomResult.Success(user)
            }
            is CustomResult.Failure -> CustomResult.Failure(userResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(userResult.progress)
        }
    }

}
