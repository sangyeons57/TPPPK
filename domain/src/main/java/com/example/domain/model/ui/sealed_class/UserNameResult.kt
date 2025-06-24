package com.example.domain.model.ui.sealed_class

/**
 * Represents the result of a nickname validation attempt, specifically for sign-up.
 * It can indicate success (Valid) or various types of failures based on nickname rules.
 * 이 모델은 UI 계층에서 닉네임 검증 결과를 표시하는데 사용됩니다.
 */
sealed class UserNameResult {
    /** Indicates the nickname is valid for sign-up. */
    object Valid : UserNameResult()

    /** Indicates the nickname field was empty. */
    object Empty : UserNameResult()

    /** Indicates the nickname is too short. */
    data class TooShort(val minimumLength: Int) : UserNameResult()

    /** Indicates the nickname is too long. */
    data class TooLong(val maximumLength: Int) : UserNameResult()

    /** Indicates the nickname contains invalid characters. */
    object InvalidCharacters : UserNameResult() // Could be more specific if needed

    /** Indicates the nickname is already taken. */
        object NicknameAlreadyExists : UserNameResult()

    /** Indicates a failure during validation, e.g., a network error. */
    data class Failure(val message: String) : UserNameResult()

}
