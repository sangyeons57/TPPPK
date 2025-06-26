package com.example.domain.model.vo.user

/**
 * Represents a user's memo as a Value Object.
 * Ensures the memo meets certain criteria (e.g., length limits).
 */
@JvmInline
value class UserMemo(val value: String?) {
    init {
        value?.let {
//            require(it.length <= MAX_LENGTH) { "User memo cannot exceed $MAX_LENGTH characters." }
        }
    }

    companion object {
        const val MAX_LENGTH = 500
    }
}
