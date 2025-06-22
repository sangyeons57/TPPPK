package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Loading.getOrDefault
import com.example.core_common.result.CustomResult.Loading.getOrElse
import com.example.core_common.result.CustomResult.Loading.getOrThrow
import com.example.core_common.result.resultTry
import com.example.domain.event.DomainEventPublisher
import com.example.domain.model.base.User
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

interface SuspendAccountUseCase {
    /**
     * Invokes the use case.
     *
     * @return [CustomResult.Success] when the account suspension workflow completes, or
     *         [CustomResult.Failure] wrapping the underlying [Exception] on failure.
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception>
}

class SuspendAccountUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val domainEventPublisher: DomainEventPublisher,
) : SuspendAccountUseCase {

    override suspend operator fun invoke(): CustomResult<Unit, Exception> = resultTry {
        // Step 1: Ensure there is a logged-in user and obtain their ID.
        val session = authRepository.getCurrentUserSession().getOrThrow()

        val user = when (val result =userRepository.findById(DocumentId.from(session.userId))){
            is CustomResult.Success -> result.data as User
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }
        user.suspendAccount()

            // Step 4: Persist via domain-port save().
        return userRepository.save(user).suspendSuccessProcess {
            // Step 5: Publish domain events.
            domainEventPublisher.publish(user)
            CustomResult.Success(Unit)
        }
    }
}
