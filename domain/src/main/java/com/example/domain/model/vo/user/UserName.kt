package com.example.domain.model.vo.user

import com.example.domain.model.vo.Name

/**
 * Represents a user's name as a Value Object.
 * Ensures the name meets certain criteria (e.g., not empty, length limits).
 */
@JvmInline
value class UserName(val value: String) {
    init {
//        require(value.isNotBlank()) { "User name cannot be blank." }
//        require(value.length <= MAX_LENGTH) { "User name cannot exceed $MAX_LENGTH characters." }
        // Add other validation rules if necessary (e.g., no special characters)
    }

    companion object {
        const val MAX_LENGTH = 50
        val UNKNOWN_USER = UserName("Unknown User")
        val EMPTY = UserName("")

        fun from(value: Name): UserName {
            return UserName(value.value)
        }

        fun from(value: String): UserName {
            return UserName(value)
        }
    }

    fun contains(query: String, ignoreCase: Boolean = true): Boolean {
        return value.contains(query, ignoreCase)
    }

    fun trim(): UserName {
        return UserName(value.trim())
    }

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }

    val length: Int
        get() = value.length

    fun matches(regex: Regex): Boolean {
        return regex.matches(value)
    }
}
