package com.example.domain.model.vo

/**
 * Dedicated identifier for User aggregates. Separate from generic [DocumentId]
 * to allow semantic clarity and potential specialized validation.
 */
@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank." }
        require(value.length <= MAX_LENGTH) { "UserId cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 128

        val UNKNOWN_USER = UserId("UNKNOWN_USER")
        val EMPTY = UserId("")

        fun from (value: String): UserId {
            return UserId(value)
        }
        fun from (value: DocumentId): UserId {
            return UserId(value.value)
        }

    }

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }
}
