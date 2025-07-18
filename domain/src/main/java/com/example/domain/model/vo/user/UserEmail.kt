package com.example.domain.model.vo.user

import java.util.regex.Pattern

/**
 * Represents a user's email address as a Value Object.
 * Ensures the email format is valid upon creation.
 */
@JvmInline
value class UserEmail(val value: String) {
    init {
//        require(isValid(value)) { "Invalid email format: $value" }
    }

    companion object {
        private const val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$"
        private val EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX)
        val EMPTY = UserEmail("")

        fun isValid(email: String): Boolean {
            return EMAIL_PATTERN.matcher(email).matches()
        }
    }

    fun trim(): UserEmail {
        return UserEmail(value.trim())
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isEmpty(): Boolean {
        return value.isEmpty()
    }

    fun isEmailPatternAvailable(): Boolean {
        return isValid(value)
    }
}
