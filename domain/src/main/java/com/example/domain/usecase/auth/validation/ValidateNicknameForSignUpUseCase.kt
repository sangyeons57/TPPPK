package com.example.domain.usecase.auth.validation

import com.example.domain.model.ui.sealed_class.UserNameResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

/**
 * Use case to validate a nickname for the sign-up process.
 * It checks for emptiness, length, allowed characters, and availability.
 *
 * @property userRepository Repository for user-related data operations.
 */
class ValidateNicknameForSignUpUseCase @Inject constructor(
    private val userRepository: LocalUserRepository
) {

    companion object {
        private const val MIN_NICKNAME_LENGTH = 3
        private const val MAX_NICKNAME_LENGTH = 20
        private val ALLOWED_NICKNAME_REGEX = "^[a-zA-Z0-9]*$".toRegex() // Alphanumeric
    }

    /**
     * Validates the given nickname string for sign-up.
     *
     * @param username The nickname string to validate.
     * @return A [UserNameResult] indicating the outcome of the validation.
     */
    suspend operator fun invoke(username: UserName): UserNameResult {
        // TODO: Implement ValidateNicknameForSignUpUseCase using LocalUserRepository
        // This should:
        // 1. Validate nickname format: check if blank, length (3-20 chars), alphanumeric only
        // 2. Return appropriate UserNameResult for format validation (Empty, TooShort, TooLong, InvalidCharacters)
        // 3. Check nickname availability using userRepository.observeByName(username).first()
        // 4. Handle CustomResult states: Success (nickname taken), Failure (check NoSuchElementException for availability)
        // 5. Return UserNameResult.NicknameAlreadyExists if user found
        // 6. Return UserNameResult.Valid if NoSuchElementException (nickname available)
        // 7. Handle other CustomResult states (Loading, Initial, Progress) with appropriate failure messages
        // 8. Wrap in try-catch for Flow collection exceptions
        // Note: This should work with LocalUserRepository for local data validation
        TODO("ValidateNicknameForSignUpUseCase implementation pending - convert to use LocalUserRepository for SSOT pattern")
    }
}
