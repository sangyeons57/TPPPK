package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.event.DomainEventPublisher
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

    override suspend operator fun invoke(): CustomResult<Unit, Exception> {
        // Step 1: Ensure there is a logged-in user and obtain their ID.
        val sessionResult = authRepository.getCurrentUserSession()
        return when (sessionResult) {
            is CustomResult.Success -> {
                val userId = sessionResult.data.userId

                // Step 2: Fetch aggregate via new domain-port API.
                val userResult = userRepository.findById(userId)
                when (userResult) {
                    is CustomResult.Success -> {
                        val user = userResult.data

                        // Step 3: Apply business action inside the aggregate.
                        user.suspendAccount()

                        // Step 4: Persist via domain-port save().
                        val updateResult = userRepository.save(user)
                        when (updateResult) {
                            is CustomResult.Success -> {
                                // Step 5: Publish domain events.
                                user.pullDomainEvents().forEach { domainEventPublisher.publish(it) }
                                CustomResult.Success(Unit)
                            }
                            is CustomResult.Failure -> updateResult
                            CustomResult.Initial -> TODO()
                            CustomResult.Loading -> TODO()
                            is CustomResult.Progress -> TODO()
                        }
                    }
                    is CustomResult.Failure -> userResult
                    CustomResult.Initial -> TODO()
                    CustomResult.Loading -> TODO()
                    is CustomResult.Progress -> TODO()
                }
            }
            is CustomResult.Failure -> sessionResult
            CustomResult.Initial -> TODO()
            CustomResult.Loading -> TODO()
            is CustomResult.Progress -> TODO()
        }
    }
}
