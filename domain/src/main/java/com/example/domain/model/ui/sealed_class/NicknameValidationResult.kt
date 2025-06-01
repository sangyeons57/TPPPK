package com.example.domain.model.ui.auth

/**
 * Represents the result of a nickname validation attempt, specifically for sign-up.
 * It can indicate success (Valid) or various types of failures based on nickname rules.
 * 이 모델은 UI 계층에서 닉네임 검증 결과를 표시하는데 사용됩니다.
 */
sealed class NicknameValidationResult {
    /** Indicates the nickname is valid for sign-up. */
    object Valid : NicknameValidationResult()

    /** Indicates the nickname field was empty. */
    object Empty : NicknameValidationResult()

    /** Indicates the nickname is too short. */
    data class TooShort(val minimumLength: Int) : NicknameValidationResult()

    /** Indicates the nickname is too long. */
    data class TooLong(val maximumLength: Int) : NicknameValidationResult()

    /** Indicates the nickname contains invalid characters. */
    object InvalidCharacters : NicknameValidationResult() // Could be more specific if needed

    /** Indicates the nickname is already taken. */
    object NicknameAlreadyExists : NicknameValidationResult()

    /**
     * Represents a generic failure during validation, potentially due to network issues
     * or other unexpected errors when checking for nickname existence or validity.
     * @param message A descriptive error message.
     */
    data class Failure(val message: String?) : NicknameValidationResult()
}
